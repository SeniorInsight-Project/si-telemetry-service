package com.seniorinsight.telemetry.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;

@Entity
@Table(name="telemetria_wearable", indexes={@Index(name="idx_telemetria_wearable_idoso_coletado",columnList="idoso_id, coletado_em DESC"),@Index(name="idx_telemetria_wearable_coletado_em",columnList="coletado_em DESC"),@Index(name="idx_telemetria_wearable_dispositivo",columnList="dispositivo_id")})
public class TelemetriaWearable {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="idoso_id",nullable=false) private UUID idosoId;
 @Column(name="dispositivo_id",length=100) private String dispositivoId;
 @Column(name="modelo_dispositivo",length=100) private String modeloDispositivo;
 @Column(name="provedor",length=50) private String provedor;
 @Column(name="frequencia_cardiaca") private Integer frequenciaCardiaca;
 @Column(name="frequencia_cardiaca_repouso") private Integer frequenciaCardiacaRepouso;
 @Column(name="variabilidade_cardiaca") private Double variabilidadeCardiaca;
 private Integer passos;
 @Column(name="distancia_metros") private Double distanciaMetros;
 @Column(name="calorias_queimadas") private Double caloriasQueimadas;
 @Column(name="queda_detectada",nullable=false) private Boolean quedaDetectada=false;
 @Column(name="forca_impacto_g") private Double forcaImpactoG;
 @Column(name="confianca_queda") private Double confiancaQueda;
 @Column(name="nivel_bateria") private Integer nivelBateria;
 @Column(name="saturacao_oxigenio") private Double saturacaoOxigenio;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="dados_brutos",columnDefinition="jsonb") private Map<String,Object> dadosBrutos=new HashMap<>();
 @Column(name="coletado_em",nullable=false) private Instant coletadoEm;
 @Column(name="criado_em",nullable=false,updatable=false) private Instant criadoEm;
 protected TelemetriaWearable() {}
 public static TelemetriaWearable from(UUID idosoId,String deviceId,String model,String provider,Integer bpm,Integer resting,Double hrv,Integer steps,Double distance,Double calories,Boolean fall,Double impact,Double confidence,Integer battery,Double spo2,Map<String,Object> raw,Instant collected){var t=new TelemetriaWearable();t.idosoId=idosoId;t.dispositivoId=deviceId;t.modeloDispositivo=model;t.provedor=provider;t.frequenciaCardiaca=bpm;t.frequenciaCardiacaRepouso=resting;t.variabilidadeCardiaca=hrv;t.passos=steps;t.distanciaMetros=distance;t.caloriasQueimadas=calories;t.quedaDetectada=fall!=null&&fall;t.forcaImpactoG=impact;t.confiancaQueda=confidence;t.nivelBateria=battery;t.saturacaoOxigenio=spo2;t.dadosBrutos=raw==null?new HashMap<>():new HashMap<>(raw);t.coletadoEm=collected==null?Instant.now():collected;return t;}
 @PrePersist void onCreate(){if(criadoEm==null)criadoEm=Instant.now();if(coletadoEm==null)coletadoEm=Instant.now();if(quedaDetectada==null)quedaDetectada=false;}
 public UUID getId(){return id;} public UUID getIdosoId(){return idosoId;} public String getDispositivoId(){return dispositivoId;} public String getModeloDispositivo(){return modeloDispositivo;} public String getProvedor(){return provedor;} public Integer getFrequenciaCardiaca(){return frequenciaCardiaca;} public Integer getFrequenciaCardiacaRepouso(){return frequenciaCardiacaRepouso;} public Double getVariabilidadeCardiaca(){return variabilidadeCardiaca;} public Integer getPassos(){return passos;} public Double getDistanciaMetros(){return distanciaMetros;} public Double getCaloriasQueimadas(){return caloriasQueimadas;} public Boolean getQuedaDetectada(){return quedaDetectada;} public Double getForcaImpactoG(){return forcaImpactoG;} public Double getConfiancaQueda(){return confiancaQueda;} public Integer getNivelBateria(){return nivelBateria;} public Double getSaturacaoOxigenio(){return saturacaoOxigenio;} public Map<String,Object> getDadosBrutos(){return dadosBrutos;} public Instant getColetadoEm(){return coletadoEm;} public Instant getCriadoEm(){return criadoEm;}
}
