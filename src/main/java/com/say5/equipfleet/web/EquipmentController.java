package com.say5.equipfleet.web;

import com.say5.equipfleet.domain.Equipment;
import com.say5.equipfleet.domain.UsageEvent;
import com.say5.equipfleet.service.EquipmentService;
import com.say5.equipfleet.web.Dtos.EquipmentResponse;
import com.say5.equipfleet.web.Dtos.EventResponse;
import com.say5.equipfleet.web.Dtos.RecordEventRequest;
import com.say5.equipfleet.web.Dtos.RegisterEquipmentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment")
class EquipmentController {

  private final EquipmentService equipmentService;

  EquipmentController(EquipmentService equipmentService) {
    this.equipmentService = equipmentService;
  }

  @PostMapping
  ResponseEntity<EquipmentResponse> register(@Valid @RequestBody RegisterEquipmentRequest request) {
    Equipment created = equipmentService.register(request.type(), request.site());
    return ResponseEntity.status(HttpStatus.CREATED).body(EquipmentResponse.from(created));
  }

  @GetMapping
  List<EquipmentResponse> list() {
    return equipmentService.list().stream().map(EquipmentResponse::from).toList();
  }

  @GetMapping("/{id}")
  EquipmentResponse get(@PathVariable Long id) {
    return EquipmentResponse.from(equipmentService.get(id));
  }

  @PostMapping("/{id}/events")
  ResponseEntity<EventResponse> recordEvent(
      @PathVariable Long id, @Valid @RequestBody RecordEventRequest request) {
    UsageEvent event = equipmentService.recordEvent(id, request.status(), request.occurredAt());
    return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
  }

  @GetMapping("/{id}/events")
  List<EventResponse> history(@PathVariable Long id) {
    return equipmentService.history(id).stream().map(EventResponse::from).toList();
  }
}
