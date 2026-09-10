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

    // 새 택배를 접수하고 최초 이력을 남긴다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType)
            throws ParcelException, SQLException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, LocalDate.now());
        parcel.expectedDeliveryDate = parcel.registeredDate.plusDays(parcel.getExpectedDeliveryDays());
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, ParcelStatus afterStatus)
            throws ParcelException, SQLException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.status != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.status, afterStatus);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
    }

    // DB에 저장된 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() throws SQLException {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.trackingNumber + " / " + parcel.receiverName
                    + " / " + parcel.getDeliveryType() + " / " + parcel.status);
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
            parcelStatement.setString(1, parcel.trackingNumber);
            parcelStatement.setString(2, parcel.receiverName);
            parcelStatement.setString(3, parcel.receiverPhoneNumber);
            parcelStatement.setString(4, parcel.destination);
            parcelStatement.setInt(5, parcel.weight);
            parcelStatement.setString(6, parcel.getDeliveryType());
            parcelStatement.setInt(7, parcel.calculateFee());
            parcelStatement.setString(8, parcel.status.toString());
            parcelStatement.setString(9, parcel.registeredDate.toString());
            parcelStatement.setString(10, parcel.expectedDeliveryDate.toString());
            parcelStatement.executeUpdate();
        }

        deleteHistories(parcel.trackingNumber);
        insertHistories(parcel);
        connection.commit();
        connection.setAutoCommit(true);
    }

    // 운송장 번호에 해당하는 택배와 배송 이력을 조회한다.
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
            for (DeliveryHistory history : parcel.histories) {
                statement.setString(1, parcel.trackingNumber);
                statement.setString(2, history.beforeStatus.toString());
                statement.setString(3, history.afterStatus.toString());
                statement.setString(4, history.changedAt.toString());
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

        parcel.status = ParcelStatus.valueOf(resultSet.getString("status"));
        parcel.expectedDeliveryDate = LocalDate.parse(resultSet.getString("expected_delivery_at"));
        return parcel;
    }

    // 택배의 배송 이력을 DB에서 읽어 온다.
    private void loadHistories(Parcel parcel) throws SQLException {
        String sql = "SELECT * FROM delivery_histories "
                + "WHERE tracking_number = ? ORDER BY history_id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parcel.trackingNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    parcel.histories.add(new DeliveryHistory(
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
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    ParcelStatus status = ParcelStatus.접수;
    LocalDate registeredDate;
    LocalDate expectedDeliveryDate;
    List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.weight = weight;
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus) {
        histories.add(new DeliveryHistory(beforeStatus, afterStatus, LocalDateTime.now()));
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int fee = 3000;
        if (weight >= 3) {
            fee += 2000;
        }
        if (destination.equals("제주")) {
            fee += 3000;
        }
        return fee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
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
    ParcelStatus beforeStatus;
    ParcelStatus afterStatus;
    LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus, LocalDateTime changedAt) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedAt = changedAt;
    }
}
