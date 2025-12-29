package com.example.bookingservice.controllers;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingStatus;
import com.example.bookingservice.services.BookingService;
import com.example.bookingservice.util.BookingErrorResponse;
import com.example.bookingservice.util.BookingException;
import com.example.bookingservice.util.ErrorsUtil;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${application.endpoint.root}")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final ModelMapper modelMapper;

    @Value("${application.endpoint.root}")
    private String rootEndpointUri;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    @GetMapping
    public ResponseEntity<Page<GetBookingDTO>> getAllBookings(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
                                                              @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getAllBookings(jwtToken, pageable));
    }

    @GetMapping(path = "${application.endpoint.id}")
    public ResponseEntity<GetBookingDTO> getBookingById(@PathVariable("id") Long id,
                                                        @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader){
        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getBookingById(id, jwtToken));
    }

    @GetMapping("/property/{id}")
    public ResponseEntity<Page<GetBookingDTO>> getBookingsByPropertyId(
            @PathVariable("id") Long propertyId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
            @PageableDefault(size = 10, sort = "checkInDate", direction = Sort.Direction.DESC) Pageable pageable){

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getBookingByPropertyId(propertyId, jwtToken, pageable));
    }

    @GetMapping(path = "${application.endpoint.booking-history-by-id}")
    public ResponseEntity<BookingHistoryResponse> getBookingHistoryById(@PathVariable("id") Long id){
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingHistoryResponse(bookingService.getBookingHistoryByBookingId(id)));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<GetBookingDTO>> getRecentBookings(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {

        String jwtToken = authorizationHeader.replace("Bearer ", "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookingService.getUserRecentBookings(jwtToken));
    }

    @PostMapping
    public ResponseEntity<GetBookingDTO> createBooking(@Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
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
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader,
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

    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<Map<String, String>> initPayment(
            @PathVariable Long bookingId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {

        String jwtToken = authorizationHeader.replace("Bearer ", "");

        bookingService.initiatePayment(bookingId, jwtToken);

        return ResponseEntity.ok(Map.of("paymentUrl", frontendUrl + bookingId));
    }

    @PostMapping("/{bookingId}/payment/success")
    public ResponseEntity<Void> handlePaymentSuccess(@PathVariable Long bookingId) {
        bookingService.completePayment(bookingId);
        return ResponseEntity.ok().build();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id,
                                              @Parameter(hidden = true) @RequestHeader("Authorization") String authorizationHeader) {
        String jwtToken = authorizationHeader.replace("Bearer ", "");

        bookingService.cancelBooking(id, jwtToken);

        return ResponseEntity.noContent().build();
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