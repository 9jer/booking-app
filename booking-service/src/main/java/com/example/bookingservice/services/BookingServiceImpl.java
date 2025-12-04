package com.example.bookingservice.services;

import com.example.bookingservice.client.PropertyClient;
import com.example.bookingservice.client.UserClient;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.dto.GetPropertyDTO;
import com.example.bookingservice.event.BookingCreatedEvent;
import com.example.bookingservice.event.BookingCreatedEventProducer;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.repositories.BookingHistoryRepository;
import com.example.bookingservice.repositories.BookingRepository;
import com.example.bookingservice.util.BookingException;
import com.example.bookingservice.util.JwtTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;
    private final JwtTokenUtils jwtTokenUtils;
    private final BookingCreatedEventProducer producer;
    private final ModelMapper modelMapper;
    private final TransactionTemplate transactionTemplate;

    private static final int MAX_BOOKING_WINDOW_MONTHS = 3;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              BookingHistoryRepository bookingHistoryRepository,
                              PropertyClient propertyClient,
                              UserClient userClient,
                              JwtTokenUtils jwtTokenUtils,
                              BookingCreatedEventProducer producer,
                              ModelMapper modelMapper,
                              PlatformTransactionManager transactionManager) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.propertyClient = propertyClient;
        this.userClient = userClient;
        this.jwtTokenUtils = jwtTokenUtils;
        this.producer = producer;
        this.modelMapper = modelMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Page<GetBookingDTO> getAllBookings(String token, Pageable pageable) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        Page<Booking> bookings;

        if (roles.contains("ROLE_ADMIN")) {
            bookings = bookingRepository.findAll(pageable);
        } else {
            Long currentUserId = jwtTokenUtils.getUserId(token);
            bookings = bookingRepository.findByUserId(currentUserId, pageable);
        }

        return bookings.map(this::convertToGetBookingDTO);
    }

    @Override
    @Cacheable(value = "bookingById", key = "#bookingId")
    public GetBookingDTO getBookingById(Long bookingId, String token) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !booking.getUserId().equals(currentUserId)) {
            GetPropertyDTO property = propertyClient.getPropertyById(booking.getPropertyId());
            if (!property.getOwnerId().equals(currentUserId)) {
                throw new BookingException("You do not have permission to view this booking.");
            }
        }

        return convertToGetBookingDTO(booking);
    }

    @Override
    public Page<GetBookingDTO> getBookingByPropertyId(Long propertyId, String token, Pageable pageable) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains("ROLE_ADMIN")) {
            Long currentUserId = jwtTokenUtils.getUserId(token);
            GetPropertyDTO property = propertyClient.getPropertyById(propertyId);

            if (property == null) {
                throw new BookingException("Property not found");
            }

            if (!property.getOwnerId().equals(currentUserId)) {
                throw new BookingException("You do not have permission to view bookings for this property.");
            }
        }

        return bookingRepository.findByPropertyId(propertyId, pageable)
                .map(this::convertToGetBookingDTO);
    }

    @Override
    public List<BookingHistoryDTO> getBookingHistoryByBookingId(Long bookingId) {
        return bookingHistoryRepository.findByBookingId(bookingId).stream()
                .map(this::convertToBookingHistoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Retryable(
            retryFor = { CannotSerializeTransactionException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @CacheEvict(value = "availableDates", key = "#booking.propertyId")
    public GetBookingDTO createBooking(Booking booking, String token) {
        booking.setId(null);

        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }

        validateBookingDates(booking.getCheckInDate(), booking.getCheckOutDate());

        Boolean propertyExists = propertyClient.propertyExists(booking.getPropertyId());
        if (propertyExists == null || !propertyExists) {
            throw new BookingException("Property with id " + booking.getPropertyId() + " not found.");
        }

        Long userId = jwtTokenUtils.getUserId(token);
        booking.setUserId(userId);

        Boolean userExists = userClient.userExists(booking.getUserId());
        if (userExists == null || !userExists) {
            throw new BookingException("User with id " + booking.getUserId() + " not found.");
        }

        return transactionTemplate.execute(status -> {
            if (!isAvailable(booking.getPropertyId(), booking.getCheckInDate(), booking.getCheckOutDate())) {
                throw new BookingException("Property is not available for selected dates.");
            }

            booking.setCreatedAt(LocalDateTime.now());
            Booking savedBooking = bookingRepository.save(booking);
            saveHistory(savedBooking, booking.getStatus());

            if (savedBooking.getStatus() == BookingStatus.CONFIRMED) {
                executeAfterCommit(() -> {
                    try {
                        sendNotification(savedBooking, token);
                    } catch (Exception e) {
                        log.error("Failed to send notification for booking " + savedBooking.getId(), e);
                    }
                });
            }
            return convertToGetBookingDTO(savedBooking);
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CacheEvict(value = "availableDates", key = "#result.propertyId")
    @CachePut(value = "bookingById", key = "#bookingId")
    public GetBookingDTO updateBookingStatus(Long bookingId, BookingStatus bookingStatus, String token) {

        Booking booking = transactionTemplate.execute(status ->
                bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new BookingException("Booking not found"))
        );

        if (booking.getStatus() == bookingStatus) {
            return convertToGetBookingDTO(booking);
        }

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains("ROLE_ADMIN")) {
            Long currentUserId = jwtTokenUtils.getUserId(token);
            GetPropertyDTO property = propertyClient.getPropertyById(booking.getPropertyId());

            if (!property.getOwnerId().equals(currentUserId)) {
                throw new BookingException("You are not authorized to manage bookings for this property.");
            }
        }

        return transactionTemplate.execute(status -> {
            Booking attachedBooking = bookingRepository.findById(bookingId).orElseThrow();

            attachedBooking.setStatus(bookingStatus);
            attachedBooking.setUpdatedAt(LocalDateTime.now());
            saveHistory(attachedBooking, bookingStatus);

            Booking updatedBooking = bookingRepository.save(attachedBooking);

            if (updatedBooking.getStatus() == BookingStatus.CONFIRMED) {
                executeAfterCommit(() -> {
                    try {
                        sendNotification(updatedBooking, token);
                    } catch (Exception e) {
                        log.error("Failed to send notification for booking " + updatedBooking.getId(), e);
                    }
                });
            }
            return convertToGetBookingDTO(updatedBooking);
        });
    }

    @Override
    public Boolean isAvailable(Long propertyId, LocalDate checkIn, LocalDate checkOut) {
        validateBookingDates(checkIn, checkOut);
        return bookingRepository.countOverlappingBookings(propertyId, checkIn, checkOut) == 0;
    }

    @Override
    public Boolean whetherThereWasABooking(Long propertyId, Long userId) {
        return !bookingRepository.findConfirmedBookingByPropertyIdAndUserId(propertyId, userId).isEmpty();
    }

    @Override
    @Cacheable(value = "availableDates", key = "#propertyId")
        public List<LocalDate> getAvailableDates(Long propertyId) {
        List<Booking> bookings = bookingRepository.findFutureBookings(propertyId, LocalDate.now());

        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(MAX_BOOKING_WINDOW_MONTHS);

        if (bookings.isEmpty()) {
            LocalDate currentDate = today;
            while (currentDate.isBefore(maxDate)) {
                availableDates.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
            return availableDates;
        }

        LocalDate currentDate = today;
        LocalDate firstBookingCheckIn = bookings.get(0).getCheckInDate();

        while (currentDate.isBefore(firstBookingCheckIn) && currentDate.isBefore(maxDate)) {
            availableDates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        for (int i = 0; i < bookings.size() - 1; i++) {
            LocalDate currentBookingCheckOut = bookings.get(i).getCheckOutDate();
            LocalDate nextBookingCheckIn = bookings.get(i + 1).getCheckInDate();

            if (currentBookingCheckOut.isAfter(maxDate)) break;

            LocalDate date = currentBookingCheckOut;
            while (date.isBefore(nextBookingCheckIn) && date.isBefore(maxDate)) {
                availableDates.add(date);
                date = date.plusDays(1);
            }
        }

        LocalDate lastBookingCheckOut = bookings.get(bookings.size() - 1).getCheckOutDate();
        while (lastBookingCheckOut.isBefore(maxDate)) {
            availableDates.add(lastBookingCheckOut);
            lastBookingCheckOut = lastBookingCheckOut.plusDays(1);
        }

        return availableDates;
    }

    private void saveHistory(Booking booking, BookingStatus bookingStatus) {
        BookingHistory bookingHistory = new BookingHistory();
        bookingHistory.setBooking(booking);
        bookingHistory.setChangedAt(LocalDateTime.now());
        bookingHistory.setStatus(bookingStatus.name());
        bookingHistoryRepository.save(bookingHistory);
    }

    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new BookingException("Check-in date must be before check-out date.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BookingException("Cannot book dates in the past.");
        }
        if (checkOut.isAfter(LocalDate.now().plusMonths(MAX_BOOKING_WINDOW_MONTHS))) {
            throw new BookingException("Cannot book more than " + MAX_BOOKING_WINDOW_MONTHS + " months in advance.");
        }
    }

    private void sendNotification(Booking booking, String token) {
        BookingCreatedEvent bookingCreatedEvent = new BookingCreatedEvent();
        bookingCreatedEvent.setBookingId(booking.getId());
        bookingCreatedEvent.setEmail(jwtTokenUtils.getEmail(token));

        try {
            String propertyTitle = propertyClient.getPropertyById(booking.getPropertyId()).getTitle();
            bookingCreatedEvent.setPropertyName(propertyTitle);
        } catch (Exception e) {
            bookingCreatedEvent.setPropertyName("Unknown Property");
        }

        bookingCreatedEvent.setCheckInDate(booking.getCheckInDate().toString());
        bookingCreatedEvent.setCheckOutDate(booking.getCheckOutDate().toString());

        producer.sendBookingCreatedEvent("booking-created", bookingCreatedEvent);
    }

    protected void executeAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    private GetBookingDTO convertToGetBookingDTO(Booking booking) {
        return modelMapper.map(booking, GetBookingDTO.class);
    }

    private BookingHistoryDTO convertToBookingHistoryDTO(BookingHistory bookingHistory) {
        BookingHistoryDTO dto = modelMapper.map(bookingHistory, BookingHistoryDTO.class);

        if (bookingHistory.getBooking() != null) {
            dto.setBookingId(bookingHistory.getBooking().getId());
        }

        return dto;
    }
}