package se.sensera.banking.client;

import se.sensera.banking.*;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.impl.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) throws UseException {
        boolean run = true;
        Scanner input = new Scanner(System.in);
        ClientServices clientServices = new ClientServices();

        while (run) {
            clientServices.startScreen();
            String chooseOption = input.nextLine();
            switch (chooseOption) {
                case "1" -> clientServices.createUser();
                case "2" -> clientServices.createAccount();
                case "3" -> clientServices.createTransaction();
                case "4" -> run = false;
                default -> System.out.println("Wrong input");
            }
        }
    }
}
