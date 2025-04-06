package com.example.bookingservice.controllers;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.BookingHistoryResponse;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.dto.BookingsResponse;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.services.BookingService;
import com.example.bookingservice.util.BookingErrorResponse;
import com.example.bookingservice.util.BookingException;
import com.example.bookingservice.util.ErrorsUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final ModelMapper modelMapper;
    @Value("${application.endpoint.root}")
    private String rootEndpointUri;

    @GetMapping
    public ResponseEntity<BookingsResponse> getAllBookings(){
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingsResponse(bookingService.getAllBookings().stream()
                        .map(this::convertBookingToBookingDTO).collect(Collectors.toList())));
    }

    @GetMapping(path = "${application.endpoint.id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable("id") Long id){
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(convertBookingToBookingDTO(bookingService.getBookingById(id)));
    }

    @GetMapping(path = "${application.endpoint.booking-history-by-id}")
    public ResponseEntity<BookingHistoryResponse> getBookingHistoryById(@PathVariable("id") Long id){
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingHistoryResponse(bookingService.getBookingHistoryByBookingId(id).stream()
                        .map(this::convertBookingHistoryToBookingHistoryDTO).collect(Collectors.toList())));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestHeader("Authorization") String authorizationHeader,
                                                 @RequestBody @Valid BookingDTO bookingDTO, BindingResult bindingResult){
        Booking booking = convertBookingDTOToBooking(bookingDTO);

        if(bindingResult.hasErrors()){
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        Booking createdBooking = bookingService.createBooking(booking, jwtToken);

        URI location = URI.create(rootEndpointUri + "/" + createdBooking.getId());
        return ResponseEntity.created(location)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createdBooking);
    }

    @PatchMapping(path = "${application.endpoint.booking-status}")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable Long id, @RequestParam BookingStatus status){
        Booking updatedBooking = bookingService.updateBookingStatus(id, status);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedBooking);
    }

    @GetMapping(path = "${application.endpoint.availability}")
    public ResponseEntity<Boolean> isAvailable(@RequestHeader("Authorization") String authorizationHeader,
                               @RequestParam("propertyId") Long propertyId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok(bookingService.isAvailable(propertyId, checkIn, checkOut, jwtToken));
    }

    // whetherThereWasABooking endpoint
    @GetMapping(path = "${application.endpoint.was-booked}")
    public ResponseEntity<Boolean> wasBooked(@RequestParam("propertyId") Long propertyId, @RequestParam("userId") Long userId){
        Boolean result = bookingService.whetherThereWasABooking(propertyId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "${application.endpoint.available-dates}")
    public ResponseEntity<List<LocalDate>> getAvailableDates(@RequestParam Long propertyId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getAvailableDates(propertyId));
    }

    private BookingDTO convertBookingToBookingDTO(Booking booking){
        BookingDTO bookingDTO = modelMapper.map(booking, BookingDTO.class);

        return bookingDTO;
    }

    private Booking convertBookingDTOToBooking(BookingDTO bookingDTO){
        Booking booking = new Booking();
        booking.setUserId(bookingDTO.getUserId());
        booking.setPropertyId(bookingDTO.getPropertyId());
        booking.setStatus(bookingDTO.getStatus());
        booking.setCheckInDate(bookingDTO.getCheckInDate());
        booking.setCheckOutDate(bookingDTO.getCheckOutDate());


        return booking;
    }

    private BookingHistory convertBookingHistoryDTOToBookingHistory(BookingHistoryDTO bookingHistoryDTO){
        return modelMapper.map(bookingHistoryDTO, BookingHistory.class);
    }

    private BookingHistoryDTO convertBookingHistoryToBookingHistoryDTO(BookingHistory bookingHistory){
        return modelMapper.map(bookingHistory, BookingHistoryDTO.class);
    }

    @ExceptionHandler
    private ResponseEntity<Object> handleException(BookingException e){
        BookingErrorResponse bookingErrorResponse = new BookingErrorResponse(
            e.getMessage(), System.currentTimeMillis()
        );

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingErrorResponse);
    }
}
