package com.seniorinsight.telemetry.service;

import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.seniorinsight.telemetry.domain.TelemetriaWearable;
import com.seniorinsight.telemetry.dto.*;
import com.seniorinsight.telemetry.event.TelemetryFallDetectedEvent;
import com.seniorinsight.telemetry.repository.TelemetriaWearableRepository;

@Service
public class WearableIntegrationService {
 private final TelemetriaWearableRepository repository; private final ApplicationEventPublisher publisher; private final RestTemplate restTemplate;
 @Value("${seniorinsight.integrations.openwearables.enabled:false}") private boolean openWearablesEnabled;
 @Value("${seniorinsight.integrations.openwearables.base-url:https://api.openwearables.io}") private String openWearablesBaseUrl;
 @Value("${seniorinsight.integrations.openwearables.api-key:}") private String openWearablesApiKey;
 public WearableIntegrationService(TelemetriaWearableRepository repository,ApplicationEventPublisher publisher,RestTemplate restTemplate){this.repository=repository;this.publisher=publisher;this.restTemplate=restTemplate;}
 @Transactional public TelemetriaWearableResponseDTO processarTelemetria(UUID idosoId,TelemetriaWearableRequestDTO r){UUID id=resolve(idosoId,r==null?null:r.idosoId());var hr=r==null?null:r.heartRate();var st=r==null?null:r.steps();var fall=r==null?null:r.fallDetection();var t=TelemetriaWearable.from(id,r==null?null:r.deviceId(),r==null?null:r.deviceModel(),r!=null&&r.provider()!=null?r.provider():"OPEN_WEARABLES",hr==null?null:hr.bpm(),hr==null?null:hr.restingBpm(),hr==null?null:hr.hrvMs(),st==null?null:st.count(),st==null?null:st.distanceMeters(),st==null?null:st.caloriesKcal(),fall!=null&&Boolean.TRUE.equals(fall.fallDetected()),fall==null?null:fall.impactForceG(),fall==null?null:fall.confidence(),r==null?null:r.batteryLevel(),r==null?null:r.spo2Percentage(),r==null?null:r.metadata(),r==null?null:r.timestamp());var saved=repository.save(t);publishFall(saved);despacharParaOpenWearables(saved);return TelemetriaWearableResponseDTO.fromEntity(saved,saved.getQuedaDetectada()?"ALERTA_QUEDA_DISPARADO":"PROCESSADO_COM_SUCESSO");}
 @Transactional public List<TelemetriaWearableResponseDTO> processarTelemetriaLote(UUID id,TelemetriaWearableBatchRequestDTO batch){if(batch==null||batch.readings()==null||batch.readings().isEmpty())throw new IllegalArgumentException("O lote de telemetrias não contém leituras.");return batch.readings().stream().map(r->processarTelemetria(id,r)).toList();}
 @Transactional public TelemetriaWearableResponseDTO processarFrequenciaCardiaca(UUID id,FrequenciaCardiacaRequestDTO r){return processarTelemetria(id,new TelemetriaWearableRequestDTO(r.idosoId(),r.deviceId(),r.deviceModel(),"OPEN_WEARABLES",r.timestamp(),new HeartRateDataDTO(r.bpm(),r.restingBpm(),r.hrvMs()),null,null,null,null,null));}
 @Transactional public TelemetriaWearableResponseDTO processarPassos(UUID id,PassosRequestDTO r){return processarTelemetria(id,new TelemetriaWearableRequestDTO(r.idosoId(),r.deviceId(),r.deviceModel(),"OPEN_WEARABLES",r.timestamp(),null,new StepsDataDTO(r.count(),r.distanceMeters(),r.caloriesKcal()),null,null,null,null));}
 @Transactional public TelemetriaWearableResponseDTO processarDeteccaoQueda(UUID id,DeteccaoQuedaRequestDTO r){Map<String,Object> metadata=new HashMap<>();if(r.latitude()!=null)metadata.put("latitude",r.latitude());if(r.longitude()!=null)metadata.put("longitude",r.longitude());if(r.observacao()!=null)metadata.put("observacao",r.observacao());return processarTelemetria(id,new TelemetriaWearableRequestDTO(r.idosoId(),r.deviceId(),r.deviceModel(),"OPEN_WEARABLES",r.timestamp(),null,null,new FallDetectionDataDTO(r.fallDetected(),r.impactForceG(),r.confidence(),r.timestamp()),r.batteryLevel(),null,metadata));}
 @Transactional(readOnly=true) public List<TelemetriaWearableResponseDTO> buscarUltimasTelemetrias(UUID id,int limite){return repository.findByIdosoIdOrderByColetadoEmDesc(id,PageRequest.of(0,Math.max(1,Math.min(limite,500)))).stream().map(t->TelemetriaWearableResponseDTO.fromEntity(t,"HISTORICO")).toList();}
 @Transactional(readOnly=true) public TelemetriaWearableResponseDTO buscarUltimaLeitura(UUID id){return repository.findTopByIdosoIdOrderByColetadoEmDesc(id).map(t->TelemetriaWearableResponseDTO.fromEntity(t,"RECENTE")).orElse(null);}
 private UUID resolve(UUID path,UUID body){UUID id=path!=null?path:body;if(id==null)throw new IllegalArgumentException("O identificador do idoso não pode ser nulo.");return id;}
 private void publishFall(TelemetriaWearable t){if(Boolean.TRUE.equals(t.getQuedaDetectada()))publisher.publishEvent(new TelemetryFallDetectedEvent(t.getId(),t.getIdosoId(),t.getColetadoEm(),t.getNivelBateria()));}
 private void despacharParaOpenWearables(TelemetriaWearable t){if(!openWearablesEnabled)return;var payload=new OpenWearablesPayloadDTO(t.getIdosoId().toString(),t.getDispositivoId(),t.getModeloDispositivo(),t.getProvedor(),"telemetry",t.getColetadoEm(),t.getFrequenciaCardiaca()==null?null:new HeartRateDataDTO(t.getFrequenciaCardiaca(),t.getFrequenciaCardiacaRepouso(),t.getVariabilidadeCardiaca()),t.getPassos()==null?null:new StepsDataDTO(t.getPassos(),t.getDistanciaMetros(),t.getCaloriasQueimadas()),new FallDetectionDataDTO(t.getQuedaDetectada(),t.getForcaImpactoG(),t.getConfiancaQueda(),t.getColetadoEm()),t.getNivelBateria(),t.getSaturacaoOxigenio(),t.getDadosBrutos());var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);if(openWearablesApiKey!=null&&!openWearablesApiKey.isBlank())headers.setBearerAuth(openWearablesApiKey);try{restTemplate.postForEntity(openWearablesBaseUrl+"/v1/telemetry",new HttpEntity<>(payload,headers),Void.class);}catch(RestClientException ignored){}
 }
}
