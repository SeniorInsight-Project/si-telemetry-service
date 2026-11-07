package com.seniorinsight.telemetry.controller;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.seniorinsight.telemetry.dto.*;
import com.seniorinsight.telemetry.service.WearableIntegrationService;
import com.seniorinsight.telemetry.service.DeviceStatusQueryService;
import com.seniorinsight.telemetry.dto.DeviceStatusResponseDTO;
import jakarta.validation.Valid;
@RestController
public class TelemetriaWearableController {
 private final WearableIntegrationService service; private final DeviceStatusQueryService deviceStatusQueryService;
 public TelemetriaWearableController(WearableIntegrationService service,DeviceStatusQueryService deviceStatusQueryService){this.service=service;this.deviceStatusQueryService=deviceStatusQueryService;}
 @PostMapping({"/api/v1/idosos/{idosoId}/telemetria-wearable","/api/v1/telemetria-wearable/ingestao"}) public ResponseEntity<TelemetriaWearableResponseDTO> ingestao(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody TelemetriaWearableRequestDTO request){var response=service.processarTelemetria(idosoId,request);return ResponseEntity.status("ALERTA_QUEDA_DISPARADO".equals(response.statusProcessamento())?HttpStatus.ACCEPTED:HttpStatus.CREATED).body(response);}
 @PostMapping({"/api/v1/idosos/{idosoId}/telemetria-wearable/lote","/api/v1/telemetria-wearable/lote"}) public ResponseEntity<List<TelemetriaWearableResponseDTO>> lote(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody TelemetriaWearableBatchRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarTelemetriaLote(idosoId,request));}
 @PostMapping({"/api/v1/idosos/{idosoId}/telemetria-wearable/frequencia-cardiaca","/api/v1/telemetria-wearable/frequencia-cardiaca"}) public ResponseEntity<TelemetriaWearableResponseDTO> frequencia(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody FrequenciaCardiacaRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarFrequenciaCardiaca(idosoId,request));}
 @PostMapping({"/api/v1/idosos/{idosoId}/telemetria-wearable/passos","/api/v1/telemetria-wearable/passos"}) public ResponseEntity<TelemetriaWearableResponseDTO> passos(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody PassosRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarPassos(idosoId,request));}
 @PostMapping({"/api/v1/idosos/{idosoId}/telemetria-wearable/deteccao-queda","/api/v1/telemetria-wearable/deteccao-queda"}) public ResponseEntity<TelemetriaWearableResponseDTO> queda(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody DeteccaoQuedaRequestDTO request){return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.processarDeteccaoQueda(idosoId,request));}
 @GetMapping("/api/v1/idosos/{idosoId}/telemetria-wearable/ultimas") public ResponseEntity<List<TelemetriaWearableResponseDTO>> ultimas(@PathVariable UUID idosoId,@RequestParam(defaultValue="30") int limite){return ResponseEntity.ok(service.buscarUltimasTelemetrias(idosoId,limite));}
 @GetMapping("/api/v1/idosos/{idosoId}/telemetria-wearable/recente") public ResponseEntity<TelemetriaWearableResponseDTO> recente(@PathVariable UUID idosoId){var response=service.buscarUltimaLeitura(idosoId);return response==null?ResponseEntity.noContent().build():ResponseEntity.ok(response);}
 @GetMapping({"/v1/idosos/{idosoId}/status", "/api/v1/telemetry/idosos/{idosoId}/status"}) public ResponseEntity<DeviceStatusResponseDTO> status(@PathVariable UUID idosoId,@RequestHeader(value="X-Authenticated-User-Id",required=false) String userId,@RequestHeader(value="X-Authenticated-Role",required=false) String role,@RequestHeader(value="Authorization",required=false) String authorization){if(userId==null||role==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();String token=authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):authorization;var response=deviceStatusQueryService.getStatus(idosoId,userId,role,token);return response==null?ResponseEntity.notFound().build():ResponseEntity.ok(response);}
}
