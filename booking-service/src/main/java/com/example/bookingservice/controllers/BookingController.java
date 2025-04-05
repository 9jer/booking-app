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
import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final ModelMapper modelMapper;

    public BookingController(BookingService bookingService, ModelMapper modelMapper) {
        this.bookingService = bookingService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public BookingsResponse getAllBookings(){
        return new BookingsResponse(bookingService.getAllBookings().stream()
                .map(this::convertBookingToBookingDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public BookingDTO getBookingById(@PathVariable("id") Long id){
        return convertBookingToBookingDTO(bookingService.getBookingById(id));
    }

    @GetMapping("/history/{id}")
    public BookingHistoryResponse getBookingHistoryById(@PathVariable("id") Long id){
        return new BookingHistoryResponse(bookingService.getBookingHistoryByBookingId(id).stream()
                .map(this::convertBookingHistoryToBookingHistoryDTO).collect(Collectors.toList()));
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

        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable Long id, @RequestParam BookingStatus status){
        Booking updatedBooking = bookingService.updateBookingStatus(id, status);

        return new ResponseEntity<>(updatedBooking, HttpStatus.OK);
    }

    @GetMapping("/availability")
    public Boolean isAvailable(@RequestHeader("Authorization") String authorizationHeader,
                               @RequestParam("propertyId") Long propertyId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return bookingService.isAvailable(propertyId, checkIn, checkOut, jwtToken);
    }

    // whetherThereWasABooking endpoint
    @GetMapping("/was-booked")
    public ResponseEntity<Boolean> wasBooked(@RequestParam("propertyId") Long propertyId, @RequestParam("userId") Long userId){
        Boolean result = bookingService.whetherThereWasABooking(propertyId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }


    @GetMapping("/available-dates")
    public List<LocalDate> getAvailableDates(@RequestParam Long propertyId) {
        return bookingService.getAvailableDates(propertyId);
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

        return new ResponseEntity<>(bookingErrorResponse, HttpStatus.BAD_REQUEST);
    }
}
