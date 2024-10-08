package by.javaguru.estore.transferservice.service;

import by.javaguru.estore.transferservice.model.TransferRestModel;

public interface TransferService {
    public boolean transfer(TransferRestModel productPaymentRestModel);
}