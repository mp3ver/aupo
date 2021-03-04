import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankInfo {

    public static String targetDirectoryPath;
    private static ArrayList<String> listOfCsv = new ArrayList<String>();
    private static String fileNameRegex = "^\\d{2}-\\d{2}-\\d{4}\\.csv?";
    private static String cardNumberRegex = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}?";

    // тип операции
    private static enum OperationTypes{
        DEPOSITION, // пополнение
        WITHDRAWAL // снятие
    }

    // статус операции
    private static enum ConfirmationStatusTypes{
        CONFIRMED, // подтверждена
        REJECTED, // отклонена
        PROCESSING // обрабатывается
    }

    // идентификатор, по которому рассчитывается сумма
    private static enum IdentifierTypes {
        ID, // идентификационный номер клиента
        CARD_NUMBER, // идентификационный номер карты
        REJECTED, // отклонённые операции
        PROCESSING // неподтверждённые операции
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String inputSrt;
        int balance, menuNum = 0;
        while(menuNum!=6){
            printMenu();
            inputSrt = in.nextLine();
            while(!isNumber(inputSrt) || Integer.parseInt(inputSrt)>6 || Integer.parseInt(inputSrt)<1){
                System.out.println("Неверный ввод");
                System.out.print("Выберите пункт меню: ");
                inputSrt = in.nextLine();
            }
            menuNum = Integer.parseInt(inputSrt);
            switch (menuNum){
                case 1:
                    System.out.print("Введите путь к целевой директории: ");
                    inputSrt = in.nextLine();
                    targetDirectoryPath = inputSrt;
                    break;
                case 2:
                    System.out.print("Введите ID клиента: ");
                    inputSrt = in.nextLine();
                    while(!isNumber(inputSrt)){
                        System.out.println("Неверный ввод");
                        System.out.print("Введите ID клиента: ");
                        inputSrt = in.nextLine();
                    }
                    balance = balanceByAllFiles(inputSrt, IdentifierTypes.ID);
                    System.out.println(String.format("Результат: %d", balance));
                    break;
                case 3:
                    System.out.print("Введите номер карты: ");
                    inputSrt = in.nextLine();
                    while(!isCardNumber(inputSrt)){
                        System.out.println("Неверный ввод");
                        System.out.print("Введите номер карты: ");
                        inputSrt = in.nextLine();
                    }
                    balance = balanceByAllFiles(inputSrt, IdentifierTypes.CARD_NUMBER);
                    System.out.println(String.format("Результат: %d", balance));
                    break;
                case 4:
                    balance = balanceByAllFiles("", IdentifierTypes.PROCESSING);
                    System.out.println(String.format("Результат: %d", balance));
                    break;
                case 5:
                    balance = balanceByAllFiles("", IdentifierTypes.REJECTED);
                    System.out.println(String.format("Результат: %d", balance));
                    break;
            }
        }
    }

    // вывод меню
    private static void printMenu(){
        System.out.println();
        if(targetDirectoryPath==null){
            System.out.println("Целевая директория: не задано");
        }
        else{
            System.out.println(String.format("Целевая директория: %s",targetDirectoryPath));
        }
        System.out.println("1. Задать целевую директорию");
        System.out.println("2. Баланс операций по указанному клиенту");
        System.out.println("3. Баланс операций по указанному номеру карты");
        System.out.println("4. Сумма неподтвержденных операций");
        System.out.println("5. Сумма отклоненных операций");
        System.out.println("6. Выход");
        System.out.print("Выберите пункт меню: ");
    }

    // Метод для получения списка csv-файлов в целевой директории
    private static void getListOfCsv() throws FileNotFoundException, NullPointerException {
            if(targetDirectoryPath.equals(null)){
                throw new NullPointerException();
            }
            File targetDirectory = new File(targetDirectoryPath);
            if(!targetDirectory.exists()){
                throw new FileNotFoundException(String.format("Директории \"%s\" не существует!",targetDirectoryPath));
            }
            listOfCsv.clear();
            for(File fileSystemObject : targetDirectory.listFiles()){
                if(fileSystemObject.isFile()){
                    // Проверяем имя файла на соответствие шаблону "ДД-ММ-ГГГГ.csv"
                    Pattern pattern = Pattern.compile(fileNameRegex);
                    Matcher matcher = pattern.matcher(fileSystemObject.getName());
                    if(matcher.matches()){
                        listOfCsv.add(fileSystemObject.getAbsolutePath());
                    }
                }
            }
    }

    // функция проверяет, представляет ли из себя строка число
    private static boolean isNumber(String str){
        if(str == null){
            return false;
        }
        try{
            Integer.parseInt(str);
        }
        catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    // функция проверяет, представляет ли из себя строка элемент перечисления
    private static <E extends Enum<E>> boolean isValidEnumValue(String str, Class<E> enumClass){
        if(str == null){
            return false;
        }
        try{
            Enum.valueOf(enumClass, str);
        }
        catch(IllegalArgumentException ex){
            return false;
        }
        return true;
    }

    // функция проверяет, представляет ли из себя строка номер банковской карты
    private static boolean isCardNumber(String str){
        if(str == null){
            return false;
        }
        Pattern pattern = Pattern.compile(cardNumberRegex);
        Matcher matcher;
        matcher = pattern.matcher(str);
        if (!matcher.matches()){
            return false;
        }
        else{
            return true;
        }
    }

    // функция проверяет строку из csv-файла на соответствие формату данных
    // если строка содержит ошибку, то возвращается её описание, иначе - null
    private static String checkRow(String[] row, int rowNumber, String fileName){
        String checkResult;
        if (row.length < 7) {
            checkResult = String.format("%s : в строке %d содержатся данные в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        if (!isValidEnumValue(row[4], OperationTypes.class)) {
            checkResult = String.format("%s : в строке %d содержится тип операции в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        if (!isValidEnumValue(row[6], ConfirmationStatusTypes.class)) {
            checkResult = String.format("%s : в строке %d содержится признак подтверждения операции в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        if(!isNumber(row[5])){
            checkResult = String.format("%s : в строке %d содержится сумма операции в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        if (!isNumber(row[0])) {
            checkResult = String.format("%s : в строке %d содержится идентификационный номер клиента в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        if (!isCardNumber(row[1])) {
            checkResult = String.format("%s : в строке %d содержится идентификационный номер карты в неверном формате", fileName, rowNumber);
            return checkResult;
        }
        return null;
    }

    // функция подсчёта баланса операций по данным из одного файла;
    // переменная identifier может содержать ID клиента, номер карты или быть пустой в зависимости от значения identifierType
    private static int balanceBySingleFile(String identifier, IdentifierTypes identifierType, String fullPath) {
        int rowNumber = 0;
        int balance = 0;
        int idFromFile, operationSum;
        int  clientId;
        String cardNumber, numberFromFile, checkResult;
        OperationTypes operationType;
        ConfirmationStatusTypes confirmationStatus;
        try {
            File csvFile = new File(fullPath);
            if (!csvFile.exists()) {
                throw new FileNotFoundException(String.format("Файл \"%s\" не существует!", fullPath));
            }
            CSVReader reader = new CSVReader(new FileReader(csvFile));
            String[] row;
            while ((row = reader.readNext()) != null) {
                rowNumber += 1;
                checkResult = checkRow(row, rowNumber, csvFile.getName());
                if(checkResult != null){
                    System.out.println(checkResult);
                    continue;
                }
                else{
                    operationType = OperationTypes.valueOf(row[4]);
                    confirmationStatus = ConfirmationStatusTypes.valueOf(row[6]);
                    operationSum = Integer.parseInt(row[5]);
                    if(operationType == OperationTypes.WITHDRAWAL){
                        operationSum *= -1; // если тип операции - "снятие", то делаем сумму операции отрицательной
                    }
                    idFromFile = Integer.parseInt(row[0]);
                    numberFromFile = row[1];
                }
                switch (identifierType) {
                    case ID:
                        clientId = Integer.parseInt(identifier);
                        if (clientId == idFromFile && confirmationStatus == ConfirmationStatusTypes.CONFIRMED) {
                            balance += operationSum;
                        }
                        break;
                    case CARD_NUMBER:
                        cardNumber = identifier;
                        if (cardNumber.equals(numberFromFile) && confirmationStatus == ConfirmationStatusTypes.CONFIRMED) {
                            balance += operationSum;
                        }
                        break;
                    case REJECTED:
                        if (confirmationStatus == ConfirmationStatusTypes.REJECTED) {
                            balance += operationSum;
                        }
                        break;
                    case PROCESSING:
                        if (confirmationStatus == ConfirmationStatusTypes.PROCESSING) {
                            balance += operationSum;
                        }
                        break;
                }

            }
        }
        catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            return 0;
        }
        catch(IOException e){
            System.out.println("Неверный формат csv");
            return 0;
        }
        catch(CsvValidationException e){
            System.out.println("Неверный формат csv");
            return 0;
        }
    return balance;
    }

    // функция подсчёта баланса операций по данным из всех файлов;
    // переменная identifier может содержать ID клиента, номер карты или быть пустой в зависимости от значения identifierType
    public static int balanceByAllFiles(String identifier, IdentifierTypes identifierType){
        try{
            getListOfCsv();
            int balance = 0;
            for(String csvFile : listOfCsv){
                balance += balanceBySingleFile(identifier, identifierType, csvFile);
            }
            return balance;
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
            return 0;
        }
        catch(NullPointerException e){
            System.out.println("Целевая директория не задана");
            return 0;
        }
    }

}
