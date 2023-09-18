import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

public class Program {
    public static void main(String[] args) {
        Core core = new Core();
        MobileApp mobileApp = new MobileApp(core.getTicketProvider(), core.getCustomerProvider());
        BusStation busStation = new BusStation(core.getTicketProvider());
        mobileApp.buyTicket("12354");
        System.out.println("Билет куплен");

        Collection<Ticket> tickets = mobileApp.getTickets();

        if (busStation.checkTicket(tickets.stream().findFirst().get().getQrcode())) {
            System.out.println("Клиент успешно прошел в автобус.");
        }
    }
}

class Core {

    private final CustomerProvider customerProvider;
    private final TicketProvider ticketProvider;
    private final PaymentProvider paymentProvider;
    private final Database database;

    public Core() {
        database = new Database();
        customerProvider = new CustomerProvider(database);
        paymentProvider = new PaymentProvider();
        ticketProvider = new TicketProvider(database, paymentProvider);
    }

    public TicketProvider getTicketProvider() {
        return ticketProvider;
    }

    public CustomerProvider getCustomerProvider() {
        return customerProvider;
    }

}

/**
 * Покупатель
 */
class Customer {

    private static int counter;

    private final int id;

    private int balance = 1000;

    private Collection<Ticket> tickets;

    {
        id = ++counter;
        tickets = new ArrayList<>();
    }

    public Collection<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(Collection<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getBalance() {
        return balance;
    }

    public void addTicket(Ticket t) {
        tickets.add(t);
    }

    public int getId() {
        return id;
    }

}

/**
 * Билет
 */
class Ticket {

    static int count;

    private final int id;

    private final int customerId;

    private final LocalDateTime dateTime;

    private final String qrcode;

    private boolean enable = true;

    public Ticket(int customerId) {
        ++count;
        this.id = count;
        this.customerId = customerId;
        this.dateTime = LocalDateTime.now();
        this.qrcode = String.format("QR%d", count);
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getQrcode() {
        return qrcode;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}

/**
 * База данных
 */
class Database {

    private static int counter;
    private Collection<Ticket> tickets = new ArrayList<>();
    private Collection<Customer> customers = new ArrayList<>();

    public Collection<Ticket> getTickets() {
        return tickets;
    }

    public Collection<Customer> getCustomers() {
        return customers;
    }

    public void addTicket(Ticket t) {
        tickets.add(t);
    }

    /**
     * Получить актуальную стоимость билета
     */
    public double getTicketAmount() {
        return 45;
    }

    /**
     * Получить идентификатор заявки на покупку билета
     */
    public int createTicketOrder(int clientId) {
        return ++counter;
    }

}

class PaymentProvider {

    public boolean buyTicket(int orderId, String cardNo, double amount) {
        //TODO: Обращение к платежному шлюзу, попытка выполнить списание средств ...
        return true;
    }

}

/**
 * Мобильное приложение
 */
class MobileApp {

    private final Customer customer;
    private final TicketProvider ticketProvider;
    private final CustomerProvider customerProvider;


    public MobileApp(TicketProvider ticketProvider, CustomerProvider customerProvider) {
        this.ticketProvider = ticketProvider;
        this.customerProvider = customerProvider;
        customer = customerProvider.getCustomer("<login>", "<password>");
    }

    public Collection<Ticket> getTickets() {
        return customer.getTickets();
    }

    public void searchTicket(LocalDateTime dateTime) {
        customer.setTickets(ticketProvider.searchTicket(customer.getId(), dateTime));
    }

    public void buyTicket(String cardNo) {
        if (ticketProvider.buyTicket(customer.getId(), cardNo, customer)) {
            Ticket n = new Ticket(customer.getId());
            customer.addTicket(n);
            ticketProvider.getDatabase().addTicket(n);
        }
    }

}

class TicketProvider {

    private final Database database;
    private final PaymentProvider paymentProvider;

    public TicketProvider(Database database, PaymentProvider paymentProvider) {
        this.database = database;
        this.paymentProvider = paymentProvider;
    }

    public Collection<Ticket> searchTicket(int clientId, LocalDateTime dateTime) {
        Collection<Ticket> tickets = new ArrayList<>();
        for (Ticket ticket : database.getTickets()) {
            if (ticket.getCustomerId() == clientId && ticket.getDateTime().getDayOfYear() == dateTime.getDayOfYear())
                tickets.add(ticket);
        }
        return tickets;
    }

    public boolean buyTicket(int clientId, String cardNo, Customer c) {
        int orderId = database.createTicketOrder(clientId);
        double amount = database.getTicketAmount();
        if (paymentProvider.buyTicket(orderId, cardNo, amount)) {
            return c.getBalance() > amount;
        }
        return false;
    }

    public boolean checkTicket(String qrcode) {
        for (Ticket ticket : database.getTickets()) {
            if (ticket.getQrcode().equals(qrcode)) {
                ticket.setEnable(false);
                return true;
            }
        }
        return false;
    }

    public Database getDatabase() {
        return database;
    }
}

class CustomerProvider {

    private Database database;

    public CustomerProvider(Database database) {
        this.database = database;
    }

    public Customer getCustomer(String login, String password) {
        return new Customer();
    }
}

/**
 * Автобусная станция
 */
class BusStation {

    private final TicketProvider ticketProvider;

    public BusStation(TicketProvider ticketProvider) {
        this.ticketProvider = ticketProvider;
    }

    public boolean checkTicket(String qrCode) {
        return ticketProvider.checkTicket(qrCode);
    }

}