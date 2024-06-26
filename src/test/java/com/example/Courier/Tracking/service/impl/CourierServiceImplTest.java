package com.example.Courier.Tracking.service.impl;

import static com.example.Courier.Tracking.helper.DistanceCalculator.calculateDistance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Courier.Tracking.constant.ResponseMessage;
import com.example.Courier.Tracking.mapper.CourierStoreEntryLogMapper;
import com.example.Courier.Tracking.model.api.request.CreateCourierRequest;
import com.example.Courier.Tracking.model.api.request.UpdateCourierLocationRequest;
import com.example.Courier.Tracking.model.api.response.CreateCourierResponse;
import com.example.Courier.Tracking.model.api.response.GetCourierStoreEntryLogResponse;
import com.example.Courier.Tracking.model.api.response.UpdateCourierLocationResponse;
import com.example.Courier.Tracking.model.dto.CourierStoreEntryLogDto;
import com.example.Courier.Tracking.model.entity.Courier;
import com.example.Courier.Tracking.model.entity.CourierLocation;
import com.example.Courier.Tracking.model.entity.CourierStoreEntryLog;
import com.example.Courier.Tracking.model.entity.Store;
import com.example.Courier.Tracking.service.repository.CourierLocationRepositoryService;
import com.example.Courier.Tracking.service.repository.CourierRepositoryService;
import com.example.Courier.Tracking.service.repository.CourierStoreEntryLogRepositoryService;
import com.example.Courier.Tracking.service.repository.StoreRepositoryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class CourierServiceImplTest {

    @InjectMocks
    private CourierServiceImpl service;

    @Mock
    private CourierRepositoryService courierRepositoryService;

    @Mock
    private CourierLocationRepositoryService courierLocationRepositoryService;

    @Mock
    private StoreRepositoryService storeRepositoryService;

    @Mock
    private CourierStoreEntryLogRepositoryService courierStoreEntryLogRepositoryService;

    @Mock
    private CourierStoreEntryLogMapper mapper;


    @Test
    public void should_Create_Courier() {
        // Given
        CreateCourierRequest request = new CreateCourierRequest();
        request.setName("John");
        request.setSurname("Doe");

        when(courierRepositoryService.save(any())).thenReturn(any());

        // When
        CreateCourierResponse response = service.createCourier(request);

        // Then
        assertEquals(HttpStatus.OK.name(), response.getCode());
        assertEquals(ResponseMessage.SUCCESS_MESSAGE, response.getMessage());

        verify(courierRepositoryService, times(1)).save(any());
    }

    @Test
    void should_Update_Courier_Location_Success() {

        UpdateCourierLocationRequest request = new UpdateCourierLocationRequest();
        request.setCourierId(1L);
        request.setLat(40.7128);
        request.setLng(-74.0060);

        Courier courier = new Courier();
        courier.setId(1L);

        CourierLocation courierLocation = new CourierLocation();
        courierLocation.setCourier(courier);
        courierLocation.setLatitude(request.getLat());
        courierLocation.setLongitude(request.getLng());

        Store store = new Store();
        store.setId(1L);
        store.setLatitude(40.7129);
        store.setLongitude(-74.0061);

        List<Store> stores = new ArrayList<>();
        stores.add(store);

        when(courierRepositoryService.findById(1L)).thenReturn(courier);
        when(storeRepositoryService.findAll()).thenReturn(stores);
        when(courierLocationRepositoryService.save(any())).thenReturn(courierLocation);
        when(courierStoreEntryLogRepositoryService.save(any())).thenReturn(any());

        UpdateCourierLocationResponse response = service.updateCourierLocation(request);

        assertEquals(HttpStatus.OK.name(), response.getCode());
        assertEquals("SUCCESSFUL", response.getMessage());

        verify(courierRepositoryService, times(1)).findById(1L);
        verify(storeRepositoryService, times(1)).findAll();
        verify(courierLocationRepositoryService, times(1)).save(any());
        verify(courierStoreEntryLogRepositoryService, times(1)).findFirstByCourierAndStoreOrderByEntryTimeDesc(any(),
            any());
        verify(courierStoreEntryLogRepositoryService, times(1)).save(any());

    }


    @Test
    public void should_Get_Total_Travel_Distance() {
        Long courierId = 1L;

        CourierLocation location1 = new CourierLocation();
        location1.setLatitude(40.7128);
        location1.setLongitude(-74.0060);

        CourierLocation location2 = new CourierLocation();
        location2.setLatitude(40.7129);
        location2.setLongitude(-74.0061);

        CourierLocation location3 = new CourierLocation();
        location3.setLatitude(40.7130);
        location3.setLongitude(-74.0062);

        List<CourierLocation> locations = new ArrayList<>();
        locations.add(location1);
        locations.add(location2);
        locations.add(location3);

        when(courierLocationRepositoryService.findByCourierIdOrderByCreationDatetimeAsc(courierId)).thenReturn(
            locations);

        double totalDistance = service.getTotalTravelDistance(courierId);

        verify(courierLocationRepositoryService, times(1)).findByCourierIdOrderByCreationDatetimeAsc(courierId);

        double expectedTotalDistance = calculateDistance(location1.getLatitude(), location1.getLongitude(),
            location2.getLatitude(), location2.getLongitude()) +
            calculateDistance(location2.getLatitude(), location2.getLongitude(),
                location3.getLatitude(), location3.getLongitude());

        assertEquals(expectedTotalDistance, totalDistance, 0.01);
    }

    @Test
    public void should_Get_Courier_Store_Entry_Log() {

        Long courierId = 1L;
        CourierStoreEntryLog log1 = new CourierStoreEntryLog();
        CourierStoreEntryLog log2 = new CourierStoreEntryLog();
        List<CourierStoreEntryLog> entryLogs = Arrays.asList(log1, log2);

        CourierStoreEntryLogDto dto1 = new CourierStoreEntryLogDto();
        CourierStoreEntryLogDto dto2 = new CourierStoreEntryLogDto();
        List<CourierStoreEntryLogDto> dtos = Arrays.asList(dto1, dto2);

        when(courierStoreEntryLogRepositoryService.findByCourierId(courierId)).thenReturn(entryLogs);

        when(mapper.toCourierStoreEntryLogDto(log1)).thenReturn(dto1);
        when(mapper.toCourierStoreEntryLogDto(log2)).thenReturn(dto2);

        GetCourierStoreEntryLogResponse response = service.getCourierStoreEntryLog(courierId);

        assertEquals(HttpStatus.OK.name(), response.getCode());
        assertEquals(ResponseMessage.SUCCESS_MESSAGE, response.getMessage());
        assertEquals(dtos, response.getData());
    }
}




