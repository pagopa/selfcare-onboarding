package it.pagopa.selfcare.service;


import java.io.File;

public interface NotificationService {


    void sendMailWithContract(File pdf, String destination, String name, String username, String productName, String token);
}
