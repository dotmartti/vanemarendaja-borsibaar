package com.borsibaar.service;

import com.borsibaar.dto.OrganizationRequestDto;
import com.borsibaar.dto.OrganizationResponseDto;
import com.borsibaar.entity.Organization;
import com.borsibaar.mapper.OrganizationMapper;
import com.borsibaar.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationMapper organizationMapper;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void create_SetsCreatedAtAndMaps() {
        OrganizationRequestDto request = new OrganizationRequestDto("Org");
        Organization entity = new Organization();
        when(organizationMapper.toEntity(request)).thenReturn(entity);
        Organization saved = new Organization(); saved.setId(3L); saved.setName("Org"); saved.setCreatedAt(OffsetDateTime.now());
        when(organizationRepository.save(entity)).thenReturn(saved);
        when(organizationMapper.toResponse(saved)).thenReturn(new OrganizationResponseDto(3L, "Org",  saved.getCreatedAt(), saved.getUpdatedAt()));

        OrganizationResponseDto dto = organizationService.create(request);
        assertEquals(3L, dto.id());
        verify(organizationRepository).save(entity);
    }

    @Test
    void getById_NotFound_Throws() {
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> organizationService.getById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getAll_ReturnsMappedList() {
        Organization o = new Organization(); o.setId(1L); o.setName("A");
        when(organizationRepository.findAll()).thenReturn(List.of(o));
        when(organizationMapper.toResponse(o)).thenReturn(new OrganizationResponseDto(1L, "A", null, OffsetDateTime.now()));
        var list = organizationService.getAll();
        assertEquals(1, list.size());
    }
}
