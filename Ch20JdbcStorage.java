import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

enum DeliveryType {
    일반,
    특급,
    냉장,
    해외
}

enum ParcelStatus {
    접수,
    출고,
    취소,
    없음
}

public class Ch20JdbcStorage {
    static Scanner scanner = new Scanner(System.in);
    // 수업용 DB 접속 정보다. 각자 환경에 맞게 수정한다.
    static final String DB_URL = "jdbc:mysql://localhost:3306/delivery?serverTimezone=Asia/Seoul";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "비밀번호 입력";
    static ParcelRepository parcelRepository;

    // ========== CH20 변경 ==========
    // 파일 저장소 대신 DB 연결과 JdbcParcelRepository를 사용한다.
    // =================================
    // JDBC 저장소로 바뀐 뒤에도 업무 메서드는 그대로 사용할 수 있음을 보여 준다.
    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            parcelRepository = new JdbcParcelRepository(connection);
        while (true) {
            try {
                printMenu();
                int menu = readInt("메뉴 선택 : ");

                switch (menu) {
                    case 0:
                        if (!parcelRepository.findAll().isEmpty()) {
                            System.out.println("현재 택배가 1개 이상 관리되고 있습니다. 정말 종료하시겠습니까?");
                            String decide = readLine("종료하려면 Y를, 취소하려면 N을 입력해주세요 : ");

                            if (!decide.equals("Y")) {
                                System.out.println("종료를 취소하고 메뉴로 돌아갑니다.");
                                break;
                            }
                        }

                        System.out.println("프로그램을 종료합니다.");
                        return;
                    case 1:
                        registerParcel();
                        break;
                    case 2:
                        findParcel();
                        break;
                    case 3:
                        printAllParcels();
                        break;
                    case 4:
                        changeParcelStatus(ParcelStatus.출고);
                        break;
                    case 5:
                        changeParcelStatus(ParcelStatus.취소);
                        break;
                    case 6:
                        printHistory();
                        break;
                    default:
                        System.out.println("올바른 메뉴 번호를 입력해주세요.");
                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
        }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    // 여러 메뉴에서 반복되는 숫자 입력을 처리한다.
    static int readInt(String message) {
        System.out.print(message);
        return Integer.parseInt(scanner.nextLine());
    }

    // 여러 메뉴에서 반복되는 문자열 입력을 처리한다.
    static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    // 택배 관리 메뉴를 출력한다.
    static void printMenu() {
        System.out.println("======= 택배 관리 시스템 =======");
        System.out.println("1. 택배 접수");
        System.out.println("2. 운송장 번호로 조회");
        System.out.println("3. 전체 택배 조회");
        System.out.println("4. 출고 처리");
        System.out.println("5. 배송 취소");
        System.out.println("6. 배송 이력 조회");
        System.out.println("0. 프로그램 종료");
        System.out.println("============================");
    }

    // 사용자에게 접수 정보를 입력받아 택배를 저장한다.
    static void registerParcel() throws ParcelException, SQLException {
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");
        String receiverName = readLine("수령인 이름 입력하세요 : ");
        String receiverPhoneNumber = readLine("수령인 연락처를 입력하세요 : ");
        String destination = readLine("배송 지역을 입력하세요 : ");

        System.out.println("1. 간편 접수(일반 배송, 1kg)");
        System.out.println("2. 상세 접수(배송 종류와 무게 직접 입력)");
        int registerType = readInt("접수 방식 : ");

        if (registerType == 1) {
            registerSimple(
                    trackingNumber,
                    receiverName,
                    receiverPhoneNumber,
                    destination
            );
            System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);
            return;
        }

        if (registerType == 2) {
            int weight = readInt("택배 무게를 입력하세요 : ");
            DeliveryType deliveryType = readDeliveryType();

            register(
                    trackingNumber,
                    receiverName,
                    receiverPhoneNumber,
                    destination,
                    weight,
                    deliveryType
            );
            System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);
            return;
        }

        throw new ParcelException("접수 방식을 다시 선택해주세요.");
    }

    // 배송 종류를 입력받아 enum 값으로 반환한다.
    static DeliveryType readDeliveryType() {
        while (true) {
            String deliveryTypeString =
                    readLine("배송 종류를 입력하세요(일반/특급/냉장/해외) : ");

            if (deliveryTypeString.equals("일반")) {
                return DeliveryType.일반;
            } else if (deliveryTypeString.equals("특급")) {
                return DeliveryType.특급;
            } else if (deliveryTypeString.equals("냉장")) {
                return DeliveryType.냉장;
            } else if (deliveryTypeString.equals("해외")) {
                return DeliveryType.해외;
            }

            System.out.println("배송 종류가 올바르지 않습니다.");
        }
    }

    // 운송장 번호를 입력받아 택배 상세 정보를 출력한다.
    static void findParcel() throws ParcelException, SQLException {
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");
        System.out.println(getDetail(trackingNumber));
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 변경한다.
    static void changeParcelStatus(ParcelStatus afterParcelStatus)
            throws ParcelException, SQLException {
        String trackingNumber =
                readLine(afterParcelStatus + "할 운송장 번호를 입력하세요 : ");

        changeStatus(trackingNumber, afterParcelStatus);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // 운송장 번호를 입력받아 해당 택배의 배송 이력을 출력한다.
    static void printHistory() throws ParcelException, SQLException {
        String trackingNumber = readLine("조회할 운송장 번호 : ");
        System.out.print(getHistoryText(trackingNumber));
    }

    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    static void registerSimple(String trackingNumber, String receiverName, String receiverPhoneNumber,
                               String destination) throws ParcelException, SQLException {
        register(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, DeliveryType.일반);
    }

    // 새 택배를 접수하고 최초 이력을 남긴다.
    static void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                         String destination, int weight, DeliveryType deliveryType)
            throws ParcelException, SQLException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        if (weight > Parcel.MAXIMUM_WEIGHT) {
            throw new ParcelException("택배 무게는 20kg을 넘을 수 없습니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, LocalDate.now());
        parcel.setExpectedDeliveryDate(parcel.getRegisteredDate().plusDays(parcel.getExpectedDeliveryDays()));
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    static void changeStatus(String trackingNumber, ParcelStatus afterParcelStatus)
            throws ParcelException, SQLException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus);
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
    }

    // 운송장 번호에 해당하는 택배 정보를 문자열로 정리해 반환한다.
    static String getDetail(String trackingNumber) throws ParcelException, SQLException {
        Parcel parcel = findParcel(trackingNumber);
        StringBuilder detail = new StringBuilder();
        detail.append("운송장 번호: ").append(parcel.getTrackingNumber()).append('\n');
        detail.append("수령인: ").append(parcel.getReceiverName()).append('\n');
        detail.append("연락처: ").append(parcel.getReceiverPhoneNumber()).append('\n');
        detail.append("배송 지역: ").append(parcel.getDestination()).append('\n');
        detail.append("배송 종류: ").append(parcel.getDeliveryType()).append('\n');
        detail.append("무게: ").append(parcel.getWeight()).append("kg\n");
        detail.append("배송비: ").append(parcel.getFee()).append("원\n");
        detail.append("접수일: ").append(parcel.getRegisteredDate()).append('\n');
        detail.append("예상 도착일: ").append(parcel.getExpectedDeliveryDate()).append('\n');
        detail.append("상태: ").append(parcel.getParcelStatus());
        return detail.toString();
    }

    // DB에 저장된 모든 택배의 요약 정보를 출력한다.
    static void printAllParcels() throws SQLException {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 택배 한 건의 배송 이력을 문자열로 정리해 반환한다.
    static String getHistoryText(String trackingNumber) throws ParcelException, SQLException {
        Parcel parcel = findParcel(trackingNumber);
        StringBuilder historyText = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (DeliveryHistory history : parcel.getHistories()) {
            historyText.append(history.getChangedAt().format(formatter));
            historyText.append(" / ").append(history.getBeforeParcelStatus());
            historyText.append(" → ").append(history.getAfterParcelStatus()).append('\n');
        }

        return historyText.toString();
    }

    // 특정 택배의 배송 이력을 출력한다.
    static void printHistory(String trackingNumber) throws ParcelException, SQLException {
        System.out.print(getHistoryText(trackingNumber));
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    static Parcel findParcel(String trackingNumber) throws ParcelException, SQLException {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        return parcel;
    }

    // 배송 종류에 맞는 택배 객체를 만든다.
    static Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                               String destination, int weight, DeliveryType deliveryType,
                               LocalDate registeredDate) {
        if (deliveryType == DeliveryType.특급) {
            Parcel parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType == DeliveryType.냉장) {
            Parcel parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType == DeliveryType.해외) {
            Parcel parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        Parcel parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
        return parcel;
    }
}

// ========== CH20 변경 ==========
// DB 작업에서 생길 수 있는 SQLException을 Repository 밖으로 전달한다.
// =================================
// 택배 저장과 조회 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배와 배송 이력을 저장하거나 갱신한다.
    void save(Parcel parcel) throws SQLException;

    // 운송장 번호로 택배와 배송 이력을 찾는다.
    Parcel findByTrackingNumber(String trackingNumber) throws SQLException;

    // DB에 저장된 모든 택배를 반환한다.
    List<Parcel> findAll() throws SQLException;
}

// ========== CH20 변경 ==========
// 택배와 이력을 MySQL DB에 저장한다.
// =================================
class JdbcParcelRepository implements ParcelRepository {
    private Connection connection;

    // JDBC 작업에 사용할 DB 연결을 받는다.
    JdbcParcelRepository(Connection connection) {
        this.connection = connection;
    }

    // ========== CH20 변경 ==========
    // PreparedStatement로 택배 행과 배송 이력 행을 저장하고, 하나의 트랜잭션으로 처리한다.
    // =================================
    // 택배와 해당 택배의 배송 이력을 하나의 작업으로 저장한다.
    public void save(Parcel parcel) throws SQLException {
        String parcelSql = "INSERT INTO parcels "
                + "(tracking_number, receiver_name, receiver_phone_number, destination, weight, "
                + "delivery_type, fee, status, registered_at, expected_delivery_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE receiver_name = VALUES(receiver_name), "
                + "receiver_phone_number = VALUES(receiver_phone_number), destination = VALUES(destination), "
                + "weight = VALUES(weight), delivery_type = VALUES(delivery_type), fee = VALUES(fee), "
                + "status = VALUES(status), registered_at = VALUES(registered_at), "
                + "expected_delivery_at = VALUES(expected_delivery_at)";

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            try (PreparedStatement parcelStatement = connection.prepareStatement(parcelSql)) {
            parcelStatement.setString(1, parcel.getTrackingNumber());
            parcelStatement.setString(2, parcel.getReceiverName());
            parcelStatement.setString(3, parcel.getReceiverPhoneNumber());
            parcelStatement.setString(4, parcel.getDestination());
            parcelStatement.setInt(5, parcel.getWeight());
            parcelStatement.setString(6, parcel.getDeliveryType().toString());
            parcelStatement.setInt(7, parcel.getFee());
            parcelStatement.setString(8, parcel.getParcelStatus().toString());
            parcelStatement.setDate(9, Date.valueOf(parcel.getRegisteredDate()));
            parcelStatement.setDate(10, Date.valueOf(parcel.getExpectedDeliveryDate()));
                parcelStatement.executeUpdate();
            }

        deleteHistories(parcel.getTrackingNumber());
            insertHistories(parcel);
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    // 운송장 번호에 해당하는 택배와 배송 이력을 조회한다.
    // ========== CH20 변경 ==========
    // DB 행을 Parcel 객체로 만들고, 별도 조회한 배송 이력까지 연결한다.
    // =================================
    public Parcel findByTrackingNumber(String trackingNumber) throws SQLException {
        String sql = "SELECT * FROM parcels WHERE tracking_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trackingNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                Parcel parcel = createParcel(resultSet);
                loadHistories(parcel);
                return parcel;
            }
        }
    }

    // DB의 모든 택배를 조회한다.
    public List<Parcel> findAll() throws SQLException {
        List<Parcel> parcels = new ArrayList<>();
        String sql = "SELECT * FROM parcels ORDER BY registered_at";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Parcel parcel = createParcel(resultSet);
                loadHistories(parcel);
                parcels.add(parcel);
            }
        }

        return parcels;
    }

    // 기존 배송 이력을 지운다.
    private void deleteHistories(String trackingNumber) throws SQLException {
        String sql = "DELETE FROM delivery_histories WHERE tracking_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trackingNumber);
            statement.executeUpdate();
        }
    }

    // 택배 객체 안의 배송 이력을 DB에 저장한다.
    private void insertHistories(Parcel parcel) throws SQLException {
        String sql = "INSERT INTO delivery_histories "
                + "(tracking_number, before_status, after_status, changed_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
        for (DeliveryHistory history : parcel.getHistories()) {
            statement.setString(1, parcel.getTrackingNumber());
                statement.setString(2, history.getBeforeParcelStatus().toString());
                statement.setString(3, history.getAfterParcelStatus().toString());
                statement.setTimestamp(4, Timestamp.valueOf(history.getChangedAt()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    // DB에서 읽은 택배 행을 배송 종류에 맞는 객체로 바꾼다.
    private Parcel createParcel(ResultSet resultSet) throws SQLException {
        String deliveryType = resultSet.getString("delivery_type");
        String trackingNumber = resultSet.getString("tracking_number");
        String receiverName = resultSet.getString("receiver_name");
        String receiverPhoneNumber = resultSet.getString("receiver_phone_number");
        String destination = resultSet.getString("destination");
        int weight = resultSet.getInt("weight");
        LocalDate registeredDate = resultSet.getDate("registered_at").toLocalDate();

        Parcel parcel;
        if (deliveryType.equals("특급")) {
            parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
        } else if (deliveryType.equals("냉장")) {
            parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
        } else if (deliveryType.equals("해외")) {
            parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
        } else {
            parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }

        parcel.setParcelStatus(ParcelStatus.valueOf(resultSet.getString("status")));
        parcel.setExpectedDeliveryDate(resultSet.getDate("expected_delivery_at").toLocalDate());
        parcel.setFee(resultSet.getInt("fee"));
        return parcel;
    }

    // 택배의 배송 이력을 DB에서 읽어 온다.
    private void loadHistories(Parcel parcel) throws SQLException {
        String sql = "SELECT * FROM delivery_histories "
                + "WHERE tracking_number = ? ORDER BY history_id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, parcel.getTrackingNumber());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                parcel.getHistories().add(new DeliveryHistory(
                            ParcelStatus.valueOf(resultSet.getString("before_status")),
                            ParcelStatus.valueOf(resultSet.getString("after_status")),
                            resultSet.getTimestamp("changed_at").toLocalDateTime()
                    ));
                }
            }
        }
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) {
        super(message);
    }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    // ========== CH06 캡슐화 유지 ==========
    // 이후 챕터에서도 운송장 번호와 접수일은 바꾸지 않고, 배송비 규칙은 상수로 관리한다.
    // =================================
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;
    private static final int HEAVY_PARCEL_FEE = 2000;
    private static final int JEJU_DELIVERY_FEE = 3000;
    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private final DeliveryType deliveryType;
    private int weight;

    // ========== CH07 상속 유지 ==========
    // 배송 종류별 계산 결과는 택배 객체의 배송비 필드에 저장한다.
    // =================================
    private int fee;
    private ParcelStatus parcelStatus = ParcelStatus.접수;
    private final LocalDate registeredDate;
    private LocalDate expectedDeliveryDate;
    private List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, DeliveryType deliveryType, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.deliveryType = deliveryType;
        setWeight(weight);
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus) {
        histories.add(new DeliveryHistory(beforeParcelStatus, afterParcelStatus, LocalDateTime.now()));
    }

    // 운송장 번호를 반환한다.
    public String getTrackingNumber() {
        return trackingNumber;
    }

    // 수령인 이름을 반환한다.
    public String getReceiverName() {
        return receiverName;
    }

    // 수령인 연락처를 반환한다.
    public String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    // 배송 지역을 반환한다.
    public String getDestination() {
        return destination;
    }

    // 접수할 때 선택한 배송 종류를 반환한다.
    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    // 택배 무게를 반환한다.
    public int getWeight() {
        return weight;
    }

    // 현재 배송 상태를 반환한다.
    public ParcelStatus getParcelStatus() {
        return parcelStatus;
    }

    // 업무 처리 결과에 따라 배송 상태를 바꾼다.
    public void setParcelStatus(ParcelStatus parcelStatus) {
        this.parcelStatus = parcelStatus;
    }

    // 접수일을 반환한다.
    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    // 예상 도착일을 반환한다.
    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    // 예상 도착일을 저장한다.
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    boolean setWeight(int weight) {
        if (weight > MAXIMUM_WEIGHT) {
            return false;
        }

        this.weight = weight;
        return true;
    }

    // 배송 이력 목록을 반환한다.
    public List<DeliveryHistory> getHistories() {
        return histories;
    }

    // 계산되어 저장된 배송비를 반환한다.
    public int getFee() {
        return fee;
    }

    // 자식 클래스가 계산한 배송비를 택배 객체에 저장한다.
    public void setFee(int fee) {
        this.fee = fee;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int fee = BASIC_DELIVERY_FEE;
        if (weight >= 3) {
            fee += HEAVY_PARCEL_FEE;
        }
        if (destination.equals("제주")) {
            fee += JEJU_DELIVERY_FEE;
        }
        return fee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, LocalDate registeredDate) {
        this(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, registeredDate);
    }

    // 일반 배송 택배를 초기화한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                DeliveryType.일반, weight, registeredDate);
        setFee(calculateFee());
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 3;
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                DeliveryType.특급, weight, registeredDate);
        setFee(calculateFee());
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                DeliveryType.냉장, weight, registeredDate);
        setFee(calculateFee());
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                DeliveryType.해외, weight, registeredDate);
        setFee(calculateFee());
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 7;
    }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    private ParcelStatus beforeParcelStatus;
    private ParcelStatus afterParcelStatus;
    private LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, LocalDateTime changedAt) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.changedAt = changedAt;
    }

    // 변경 전 상태를 반환한다.
    ParcelStatus getBeforeParcelStatus() {
        return beforeParcelStatus;
    }

    // 변경 후 상태를 반환한다.
    ParcelStatus getAfterParcelStatus() {
        return afterParcelStatus;
    }

    // 변경 시각을 반환한다.
    LocalDateTime getChangedAt() {
        return changedAt;
    }

}
