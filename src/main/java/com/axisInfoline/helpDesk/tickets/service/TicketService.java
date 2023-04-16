package com.axisInfoline.helpDesk.tickets.service;

import com.axisInfoline.helpDesk.core.domain.Count;
import com.axisInfoline.helpDesk.employee.service.EmployeeService;
import com.axisInfoline.helpDesk.tickets.domain.Ticket;
import com.axisInfoline.helpDesk.tickets.repository.TicketJpaRepository;
import com.axisInfoline.helpDesk.tickets.repository.TicketRepository;
import io.micrometer.common.util.StringUtils;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    EmployeeService employeeService;

    @Autowired
    TicketJpaRepository ticketJpaRepository;

    public String createTicket(Ticket ticket) {
        ticket.setId(ticket.getProduct().concat(ticket.getComplainantContactNo()));
        ticket.setComplaintNo(ticket.getProjectName().substring(0, 3) + "-" + getRandomNumberString());
        ticket.setComplaintDatetime(currentDateTime());
        return ticketRepository.insertTicket(ticket);
    }

    public static LocalDateTime currentDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm");
        LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.parse(dtf.format(now));
    }

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }

    public String updateTicketByAdmin(Ticket ticket, String loggedInUserId) throws Exception {
        if(employeeService.isAdmin(loggedInUserId) || employeeService.isSuperAdmin(loggedInUserId) || employeeService.isAeit(loggedInUserId)){
            //      ticketRepository.updateTicketByAdmin(ticket);
            System.out.println(ticket);
            ticket.setStatus(ticket.getStatus().toUpperCase());
            ticketJpaRepository.save(ticket);
            return "Ticket updated";
        } else {
            throw new Exception("You are not authorized to see this data");
        }
    }

    public String updateTicketByEngineer(Ticket ticket) {
        if (ticket.getStatus().equals("CLOSED")) {
            new Exception("You can't edit Closed ticket");
        }
        return ticketRepository.updateTicketByEngineer(ticket);
    }

    public LocalDateTime convertStringToLocalDateTime(String date) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(date.concat(" 00:00"), formatter);
    }

    public List<Ticket> getAllTickets(String status, String fromDate, String toDate) {
        return ticketRepository.getAllTickets(status,  convertStringToLocalDateTime(fromDate), convertStringToLocalDateTime(toDate));
    }

    public List<Ticket> getTicketsByCircle(String circle, String status, String fromDate, String toDate) {
        return ticketRepository.getTicketsByCircle(circle, status,  convertStringToLocalDateTime(fromDate), convertStringToLocalDateTime(toDate));
    }


    public List<Ticket> getAllTicketsByPhoneNo(String phone, String status, String fromDate, String toDate) {
        return ticketRepository.getAllTicketsByPhoneNo(phone, status, convertStringToLocalDateTime(fromDate), convertStringToLocalDateTime(toDate));
    }

    public String deleteTicket(String complaintNumber, String loggedInUserId) throws Exception {
        if(employeeService.isSuperAdmin(loggedInUserId)){
            return ticketRepository.deleteTicket(complaintNumber);
        } else {
            throw new Exception("You are not authorized to see this data");
        }
    }

    public LocalDateTime concatDateAndTime(String date, String time) {
        LocalDate datePart = LocalDate.parse(date);
        LocalTime timePart = LocalTime.parse(time);
        LocalDateTime dt = LocalDateTime.of(datePart, timePart);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm");
        return LocalDateTime.parse(dtf.format(dt));
    }

    public String importTickets(MultipartFile multipartFile) {
        try {
            List<Ticket> tickets = new ArrayList<>();
            DecimalFormat decfor = new DecimalFormat("0.00");
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(multipartFile.getInputStream());
            XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
            for (Row row : skipFirst(sheet)){
//            for (Row row : sheet) {
//            {
//                Row row1 = row;
                if (row.getCell(1).getCellType() == 1) {
                    Ticket ticket = new Ticket();
                    if((!StringUtils.isEmpty(row.getCell(1).getStringCellValue()))){
                        ticket.setComplaintNo(row.getCell(1).getStringCellValue());
                    }
                    if(row.getCell(3).getDateCellValue() != null){
                        ticket.setComplaintDatetime(LocalDateTime.of(LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(row.getCell(2).getDateCellValue())), LocalTime.parse(new SimpleDateFormat("HH:mm:ss").format(row.getCell(3).getDateCellValue()))));
                    } else if(row.getCell(2).getDateCellValue() != null) {
                        ticket.setComplaintDatetime(LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(row.getCell(2).getDateCellValue())).atStartOfDay());
                    }
                    if((!StringUtils.isEmpty(row.getCell(4).getStringCellValue()))){
                        ticket.setLocationCode(row.getCell(4).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(5).getStringCellValue()))){
                        ticket.setCircle(row.getCell(5).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(6).getStringCellValue()))){
                        ticket.setDivision(row.getCell(6).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(7).getStringCellValue()))){
                        ticket.setSubstation(row.getCell(7).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(8).getStringCellValue()))){
                        ticket.setComplainantName(row.getCell(8).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(9).getStringCellValue()))){
                        ticket.setComplainantDesignation(row.getCell(9).getStringCellValue());
                    }
                    if(row.getCell(10).getCellType() == 0){
                        ticket.setComplainantContactNo(String.valueOf((long) row.getCell(10).getNumericCellValue()));
                    }
                    if((!StringUtils.isEmpty(row.getCell(8).getStringCellValue()))){
                        ticket.setDefectiveItemName(row.getCell(11).getStringCellValue());
                    }
                    //StringType=1,NumberType=0,Empty = 3
                    if(row.getCell(12).getCellType() == 1){
                        ticket.setUxb1jsi364g4453780(row.getCell(12).getStringCellValue());
                    } else if(row.getCell(12).getCellType() == 0) {
                        ticket.setUxb1jsi364g4453780(String.valueOf((long) row.getCell(12).getNumericCellValue()));
                    }
                    if((!StringUtils.isEmpty(row.getCell(13).getStringCellValue()))){
                        ticket.setProblemType(row.getCell(13).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(14).getStringCellValue()))){
                        ticket.setEngineerAssigned(row.getCell(14).getStringCellValue());
                    }
                    if(row.getCell(15).getCellType() == 0){
                        ticket.setEngineerContactNo(String.valueOf((long) row.getCell(15).getNumericCellValue()));
                    }
                    if (row.getCell(16).getDateCellValue() != null) {
                        ticket.setComplaintAttemptsFirstDateAndTime(LocalDateTime.ofInstant(row.getCell(16).getDateCellValue().toInstant(), ZoneId.systemDefault()));
                    }
                    if (row.getCell(17).getDateCellValue() != null) {
                        ticket.setComplaintAttemptsSecondDateAndTime(LocalDateTime.ofInstant(row.getCell(17).getDateCellValue().toInstant(), ZoneId.systemDefault()));
                    }
                    if (row.getCell(18).getDateCellValue() != null) {
                        ticket.setComplaintAttemptsThirdDateAndTime(LocalDateTime.ofInstant(row.getCell(18).getDateCellValue().toInstant(), ZoneId.systemDefault()));
                    }
                    if(row.getCell(20).getDateCellValue() != null){
                        ticket.setComplaintDatetime(LocalDateTime.of(LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(row.getCell(19).getDateCellValue())), LocalTime.parse(new SimpleDateFormat("HH:mm:ss").format(row.getCell(20).getDateCellValue()))));
                    } else if(row.getCell(19).getDateCellValue() != null) {
                        ticket.setComplaintCompletionDatetime(LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(row.getCell(19).getDateCellValue())).atStartOfDay());
                    }
                    if((!StringUtils.isEmpty(row.getCell(21).getStringCellValue()))){
                        ticket.setStatus(row.getCell(21).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(22).getStringCellValue()))){
                        ticket.setActionTakenAndSpareUsed(row.getCell(22).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(23).getStringCellValue()))){
                        ticket.setOldSerialNoMbHddTft(row.getCell(23).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(row.getCell(24).getStringCellValue()))){
                        ticket.setNewSerialNoMbHddTft(row.getCell(24).getStringCellValue());
                    }
                    if(row.getCell(25).getCellType() == 2){
                        if(!StringUtils.isEmpty(row.getCell(1).getStringCellValue())){
                            ticket.setComplaintAttendHours(Double.valueOf(decfor.format(row.getCell(25).getNumericCellValue())));
                        }
                    }
                    if(row.getCell(26).getCellType() == 2){
                        if(!StringUtils.isEmpty(row.getCell(1).getStringCellValue())){
                            ticket.setComplaintCompletionInDays(Double.valueOf(decfor.format(row.getCell(26).getNumericCellValue())));
                        }
                    }
                    if(row.getCell(27).getCellType() == 2){
                        if(!StringUtils.isEmpty(row.getCell(1).getStringCellValue())){
                            ticket.setComplaintCompletionInHour(Double.valueOf(decfor.format(row.getCell(27).getNumericCellValue())));
                        }
                    }
                    if((!StringUtils.isEmpty(row.getCell(28).getStringCellValue()))){
                        ticket.setRemarks(row.getCell(28).getStringCellValue());
                    }
                    if((!StringUtils.isEmpty(ticket.getComplaintNo()))){
                        ticket.setId(getMd5(ticket.getComplaintNo().concat(ticket.getComplainantName() != null ? ticket.getComplainantName(): "--")));
                    }
                    ticketRepository.insertTicketForImport(ticket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Excel Imported";
    }

    public static String getMd5(String input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isValidDate(String inDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:ms");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(inDate.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    public static <T> Iterable<T> skipFirst(final Iterable<T> c) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                Iterator<T> i = c.iterator();
                i.next();
                return i;
            }
        };
    }

}
