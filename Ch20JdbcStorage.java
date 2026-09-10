import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch20JdbcStorage {
    // 수업용 DB 접속 정보다. 각자 환경에 맞게 수정한다.
    static final String DB_URL = "jdbc:mysql://localhost:3306/delivery?serverTimezone=Asia/Seoul";
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "비밀번호 입력";

    // ========== CH20 변경 ==========
    // 파일 저장소 대신 DB 연결과 JdbcParcelRepository를 사용한다.
    // =================================
    // JDBC 저장소로 바뀐 뒤에도 서비스 사용 방식은 같다는 것을 보여 준다.
    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            ParcelService parcelService = new ParcelService(new JdbcParcelRepository(connection));

            parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2, "특급");
            parcelService.changeStatus("1001", ParcelStatus.출고);
            parcelService.printAllParcels();
        } catch (ParcelException | SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }
}

// 택배 업무 규칙을 처리하는 클래스다.
class ParcelService {
    private ParcelRepository parcelRepository;

    // 업무 처리에 사용할 저장소를 받는다.
    ParcelService(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    void registerSimple(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination) throws ParcelException, SQLException {
        register(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, "일반");
    }

    // 새 택배를 접수하고 최초 이력을 남긴다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType)
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
    void changeStatus(String trackingNumber, ParcelStatus afterParcelStatus)
            throws ParcelException, SQLException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus);
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
    }

    // DB에 저장된 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() throws SQLException {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    Parcel findParcel(String trackingNumber) throws ParcelException, SQLException {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        return parcel;
    }

    // 배송 종류에 맞는 택배 객체를 만든다.
    Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination, int weight, String deliveryType,
                        LocalDate registeredDate) {
        if (deliveryType.equals("특급")) {
            Parcel parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("냉장")) {
            Parcel parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("해외")) {
            Parcel parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        Parcel parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
        return parcel;
    }
}

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

    // 택배와 해당 택배의 배송 이력을 하나의 작업으로 저장한다.
    // ========== CH20 변경 ==========
    // 택배 행을 저장한 뒤 배송 이력 행도 함께 DB에 저장한다.
    // =================================
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

        connection.setAutoCommit(false);

        try (PreparedStatement parcelStatement = connection.prepareStatement(parcelSql)) {
            parcelStatement.setString(1, parcel.getTrackingNumber());
            parcelStatement.setString(2, parcel.getReceiverName());
            parcelStatement.setString(3, parcel.getReceiverPhoneNumber());
            parcelStatement.setString(4, parcel.getDestination());
            parcelStatement.setInt(5, parcel.getWeight());
            parcelStatement.setString(6, parcel.getDeliveryType());
            parcelStatement.setInt(7, parcel.calculateFee());
            parcelStatement.setString(8, parcel.getParcelStatus().toString());
            parcelStatement.setString(9, parcel.getRegisteredDate().toString());
            parcelStatement.setString(10, parcel.getExpectedDeliveryDate().toString());
            parcelStatement.executeUpdate();
        }

        deleteHistories(parcel.getTrackingNumber());
        insertHistories(parcel);
        connection.commit();
        connection.setAutoCommit(true);
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
                statement.setString(4, history.getChangedAt().toString());
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
        LocalDate registeredDate = LocalDate.parse(resultSet.getString("registered_at"));

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
        parcel.setExpectedDeliveryDate(LocalDate.parse(resultSet.getString("expected_delivery_at")));
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
                            LocalDateTime.parse(resultSet.getString("changed_at"))
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
    private static final int HEAVY_PARCEL_SURCHARGE = 2000;
    private static final int JEJU_SURCHARGE = 3000;
    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private int weight;
    private ParcelStatus parcelStatus = ParcelStatus.접수;
    private final LocalDate registeredDate;
    private LocalDate expectedDeliveryDate;
    private List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus) {
        histories.add(new DeliveryHistory(beforeParcelStatus, afterParcelStatus, LocalDateTime.now()));
    }

    // 운송장 번호를 반환한다.
    String getTrackingNumber() {
        return trackingNumber;
    }

    // 수령인 이름을 반환한다.
    String getReceiverName() {
        return receiverName;
    }

    // 수령인 연락처를 반환한다.
    String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    // 배송 지역을 반환한다.
    String getDestination() {
        return destination;
    }

    // 택배 무게를 반환한다.
    int getWeight() {
        return weight;
    }

    // 현재 배송 상태를 반환한다.
    ParcelStatus getParcelStatus() {
        return parcelStatus;
    }

    // 업무 처리 결과에 따라 배송 상태를 바꾼다.
    void setParcelStatus(ParcelStatus parcelStatus) {
        this.parcelStatus = parcelStatus;
    }

    // 접수일을 반환한다.
    LocalDate getRegisteredDate() {
        return registeredDate;
    }

    // 예상 도착일을 반환한다.
    LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    // 예상 도착일을 저장한다.
    void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    void setWeight(int weight) {
        this.weight = weight;
    }

    // 배송 이력 목록을 반환한다.
    List<DeliveryHistory> getHistories() {
        return histories;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int deliveryFee = BASIC_DELIVERY_FEE;
        if (weight >= 3) {
            deliveryFee += HEAVY_PARCEL_SURCHARGE;
        }
        if (destination.equals("제주")) {
            deliveryFee += JEJU_SURCHARGE;
        }
        return deliveryFee;
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
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 3;
    }
    // 일반 배송 이름을 반환한다.
    String getDeliveryType() {
        return "일반";
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 특급 배송 이름을 반환한다.
    String getDeliveryType() {
        return "특급";
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 냉장 배송 이름을 반환한다.
    String getDeliveryType() {
        return "냉장";
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 7;
    }
    // 해외 배송 이름을 반환한다.
    String getDeliveryType() {
        return "해외";
    }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    private ParcelStatus beforeParcelStatus;
    private ParcelStatus afterParcelStatus;
    private LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
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

    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, LocalDateTime changedAt) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.changedAt = changedAt;
    }
}
