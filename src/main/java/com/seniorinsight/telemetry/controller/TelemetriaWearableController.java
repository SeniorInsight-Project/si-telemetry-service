package com.seniorinsight.telemetry.controller;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.seniorinsight.telemetry.dto.*;
import com.seniorinsight.telemetry.service.WearableIntegrationService;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1")
public class TelemetriaWearableController {
 private final WearableIntegrationService service; public TelemetriaWearableController(WearableIntegrationService service){this.service=service;}
 @PostMapping({"/idosos/{idosoId}/telemetria-wearable","/telemetria-wearable/ingestao"}) public ResponseEntity<TelemetriaWearableResponseDTO> ingestao(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody TelemetriaWearableRequestDTO request){var response=service.processarTelemetria(idosoId,request);return ResponseEntity.status("ALERTA_QUEDA_DISPARADO".equals(response.statusProcessamento())?HttpStatus.ACCEPTED:HttpStatus.CREATED).body(response);}
 @PostMapping({"/idosos/{idosoId}/telemetria-wearable/lote","/telemetria-wearable/lote"}) public ResponseEntity<List<TelemetriaWearableResponseDTO>> lote(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody TelemetriaWearableBatchRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarTelemetriaLote(idosoId,request));}
 @PostMapping({"/idosos/{idosoId}/telemetria-wearable/frequencia-cardiaca","/telemetria-wearable/frequencia-cardiaca"}) public ResponseEntity<TelemetriaWearableResponseDTO> frequencia(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody FrequenciaCardiacaRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarFrequenciaCardiaca(idosoId,request));}
 @PostMapping({"/idosos/{idosoId}/telemetria-wearable/passos","/telemetria-wearable/passos"}) public ResponseEntity<TelemetriaWearableResponseDTO> passos(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody PassosRequestDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.processarPassos(idosoId,request));}
 @PostMapping({"/idosos/{idosoId}/telemetria-wearable/deteccao-queda","/telemetria-wearable/deteccao-queda"}) public ResponseEntity<TelemetriaWearableResponseDTO> queda(@PathVariable(required=false) UUID idosoId,@Valid @RequestBody DeteccaoQuedaRequestDTO request){return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.processarDeteccaoQueda(idosoId,request));}
 @GetMapping("/idosos/{idosoId}/telemetria-wearable/ultimas") public ResponseEntity<List<TelemetriaWearableResponseDTO>> ultimas(@PathVariable UUID idosoId,@RequestParam(defaultValue="30") int limite){return ResponseEntity.ok(service.buscarUltimasTelemetrias(idosoId,limite));}
 @GetMapping("/idosos/{idosoId}/telemetria-wearable/recente") public ResponseEntity<TelemetriaWearableResponseDTO> recente(@PathVariable UUID idosoId){var response=service.buscarUltimaLeitura(idosoId);return response==null?ResponseEntity.noContent().build():ResponseEntity.ok(response);}
}
