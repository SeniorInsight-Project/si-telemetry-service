package com.seniorinsight.telemetry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Servidor HTTP local para testar os adaptadores de saída: devolve as respostas programadas, em ordem. */
public final class ServidorHttpFalso implements AutoCloseable {

    private final HttpServer servidor;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Queue<Resposta> respostas = new ConcurrentLinkedQueue<>();
    private final List<Requisicao> requisicoes = new CopyOnWriteArrayList<>();

    private ServidorHttpFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.setExecutor(executor);
        servidor.start();
    }

    public static ServidorHttpFalso iniciar() {
        try {
            return new ServidorHttpFalso();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public ServidorHttpFalso responder(int status, String corpoJson) {
        respostas.add(new Resposta(status, corpoJson, Duration.ZERO));
        return this;
    }

    public ServidorHttpFalso responderComAtraso(Duration atraso, int status, String corpoJson) {
        respostas.add(new Resposta(status, corpoJson, atraso));
        return this;
    }

    public List<Requisicao> requisicoes() {
        return List.copyOf(requisicoes);
    }

    @Override
    public void close() {
        servidor.stop(0);
        executor.shutdownNow();
    }

    private void atender(HttpExchange troca) throws IOException {
        String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requisicoes.add(new Requisicao(troca.getRequestMethod(), troca.getRequestURI().getPath(),
                troca.getRequestURI().getRawQuery(), troca.getRequestHeaders(), corpo));

        Resposta resposta = respostas.poll();
        if (resposta == null) {
            resposta = new Resposta(500, "{\"erro\":\"sem resposta programada\"}", Duration.ZERO);
        }
        try {
            Thread.sleep(resposta.atraso());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            troca.close();
            return;
        }

        byte[] bytes = resposta.corpo().getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "application/json");
        troca.sendResponseHeaders(resposta.status(), bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        } catch (IOException e) {
            // O cliente desistiu (timeout): nada a fazer.
        }
    }

    public record Requisicao(String metodo, String caminho, String query, Headers headers, String corpo) {

        public String header(String nome) {
            return headers.getFirst(nome);
        }
    }

    private record Resposta(int status, String corpo, Duration atraso) {
    }
}
