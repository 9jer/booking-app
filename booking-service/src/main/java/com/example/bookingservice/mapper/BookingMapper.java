package com.example.bookingservice.mapper;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.BookingHistoryDTO;
import com.example.bookingservice.dto.GetBookingDTO;
import com.example.bookingservice.models.Booking;
import com.example.bookingservice.models.BookingHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    GetBookingDTO toGetBookingDTO(Booking booking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toBooking(BookingDTO bookingDTO);

    BookingHistoryDTO toBookingHistoryDTO(BookingHistory bookingHistory);
}