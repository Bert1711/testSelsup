package ru.zaroyan;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private static final String BASE_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Lock lock;
    private int requestCount;
    private long lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.lock = new ReentrantLock();
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
    }

    public void createProductDocument(ProductDocument document, String token) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long intervalInMillis = timeUnit.toMillis(1);

            if (currentTime - lastRequestTime >= intervalInMillis) {
                // Если прошёл заданный интервал, обнуляем счетчик запросов
                requestCount = 0;
                lastRequestTime = currentTime;
            }

            if (requestCount >= requestLimit) {
                // Если лимит по количеству запросов достигнут, ждем до окончания текущего временного интервала
                Thread.sleep(intervalInMillis - (currentTime - lastRequestTime));
                requestCount = 0;
                lastRequestTime = currentTime;
            }

            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(BASE_URL + "?pg=milk");
            httpPost.setHeader("content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + token);

            ObjectMapper objectMapper = new ObjectMapper();
            String documentJson = objectMapper.writeValueAsString(document);

            HttpEntity entity = new StringEntity(documentJson, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost);
            String responseJson = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("Документ создан");
            } else {
                System.out.println("Ошибка: " + statusCode);
            }
            requestCount++;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
class ProductDocument {
    @JsonProperty("document_format")
    private String documentFormat;
    @JsonProperty("product_document")
    private String productDocument;
    @JsonProperty("product_group")
    private String productGroup;
    private String signature;
    private String type;

    // Здесь находятся конструкторы, геттеры и сеттеры для полей. Их можно сгенерировать через Lombok.
}
