package com.example.bookingservice.controllers;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.models.Booking;
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

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final ModelMapper modelMapper;
    @Value("${application.endpoint.root}")
    private String rootEndpointUri;

    @GetMapping
    public ResponseEntity<BookingsResponse> getAllBookings(@RequestHeader("Authorization") String authorizationHeader){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingsResponse(bookingService.getAllBookings(jwtToken)));
    }

    @GetMapping(path = "${application.endpoint.id}")
    public ResponseEntity<GetBookingDTO> getBookingById(@PathVariable("id") Long id,
                                                        @RequestHeader("Authorization") String authorizationHeader){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getBookingById(id, jwtToken));
    }

    @GetMapping("/property/{id}")
    public ResponseEntity<BookingsResponse> getBookingsByPropertyId(@PathVariable("id") Long propertyId,
                                                                    @RequestHeader("Authorization") String authorizationHeader){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingsResponse(bookingService.getBookingByPropertyId(propertyId, jwtToken)));
    }

    @GetMapping(path = "${application.endpoint.booking-history-by-id}")
    public ResponseEntity<BookingHistoryResponse> getBookingHistoryById(@PathVariable("id") Long id){
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingHistoryResponse(bookingService.getBookingHistoryByBookingId(id)));
    }

    @PostMapping
    public ResponseEntity<GetBookingDTO> createBooking(@RequestHeader("Authorization") String authorizationHeader,
                                                 @RequestBody @Valid BookingDTO bookingDTO, BindingResult bindingResult){
        Booking booking = convertBookingDTOToBooking(bookingDTO);

        if(bindingResult.hasErrors()){
            ErrorsUtil.returnAllErrors(bindingResult);
        }

        String jwtToken = authorizationHeader.replace("Bearer ", "");

        GetBookingDTO createdBooking = bookingService.createBooking(booking, jwtToken);

        URI location = URI.create(rootEndpointUri + "/" + createdBooking.getId());
        return ResponseEntity.created(location)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createdBooking);
    }

    @PatchMapping(path = "${application.endpoint.booking-status}")
    public ResponseEntity<GetBookingDTO> updateBookingStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestParam BookingStatus status) {

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        GetBookingDTO updatedBooking = bookingService.updateBookingStatus(id, status, jwtToken);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatedBooking);
    }

    @GetMapping(path = "${application.endpoint.availability}")
    public ResponseEntity<Boolean> isAvailable(@RequestParam("propertyId") Long propertyId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut){
        return ResponseEntity.ok(bookingService.isAvailable(propertyId, checkIn, checkOut));
    }

    @GetMapping(path = "${application.endpoint.was-booked}")
    public ResponseEntity<Boolean> wasBooked(@RequestParam("propertyId") Long propertyId, @RequestParam("userId") Long userId){
        Boolean result = bookingService.whetherThereWasABooking(propertyId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "${application.endpoint.available-dates}")
    public ResponseEntity<AvailableDatesResponse> getAvailableDates(@RequestParam Long propertyId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvailableDatesResponse(bookingService.getAvailableDates(propertyId)));
    }

    private Booking convertBookingDTOToBooking(BookingDTO bookingDTO){
        return modelMapper.map(bookingDTO, Booking.class);
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