package by.javaguru.estore.transferservice.service;

import by.javaguru.estore.transferservice.error.TransferServiceException;
import by.javaguru.estore.transferservice.model.TransferRestModel;
import by.javaguru.payments.ws.core.events.DepositRequestedEvent;
import by.javaguru.payments.ws.core.events.WithdrawalRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;

@Service
public class TransferServiceImpl implements TransferService{
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private KafkaTemplate<String, Object> kafkaTemplate;
    private Environment environment;
    private RestTemplate restTemplate;

    public TransferServiceImpl(KafkaTemplate<String, Object> kafkaTemplate, Environment environment, RestTemplate restTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.environment = environment;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
//    @Transactional(
//            value = "kafkaTransactionManager"
//            , rollbackFor = Throwable.class
//            , rollbackFor = {TransferServiceException.class, ConnectException.class}
//            , noRollbackFor = {NullPointerException.class}
//            )
    public boolean transfer(TransferRestModel transferRestModel) {

        WithdrawalRequestedEvent withdrawalRequestedEvent = new WithdrawalRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(),
                transferRestModel.getAmount());
        DepositRequestedEvent depositRequestedEvent =  new DepositRequestedEvent(transferRestModel.getSenderId(),
                transferRestModel.getRecepientId(),
                transferRestModel.getAmount());

        try {
            kafkaTemplate.send(environment.getProperty("withdraw-money-topic", "withdraw-money-topic"), withdrawalRequestedEvent);
            LOGGER.info("Sent event to withdrawal topic.");

            // Business logic that causes and error
            callRemoteService();

            kafkaTemplate.send(environment.getProperty("deposit-money-topic", "deposit-money-topic"), depositRequestedEvent);
            LOGGER.info("Sent event to deposit topic");

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new TransferServiceException(ex);
        }

        return true;
    }

    private ResponseEntity<String> callRemoteService() throws Exception{
        String url = "http://localhost:8082/response/200";

        ResponseEntity<String> response =  restTemplate.exchange(url, HttpMethod.GET, null, String.class);

        if(response.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()){
            throw new Exception("Destination Microservice not availble");
        }

        if(response.getStatusCode().value() == HttpStatus.OK.value()){
            LOGGER.info("Received response from mock service: {}", response.getBody());
        }

        return response;
    }
}
