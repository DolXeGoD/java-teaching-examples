import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
                            if (!parcelRepository.isEmpty()) {
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
                            System.out.println("없는 메뉴입니다. 다시 선택해주세요.");
                            break;
                    }
                } catch (Exception exception) {
                    System.out.println(exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    // 여러 메뉴에서 반복되는 숫자 입력 처리를 readInt로 분리한다.
    // 입력 안내 후 정수를 입력받아 반환한다.
    static int readInt(String message) {
        System.out.print(message);
        return Integer.parseInt(scanner.nextLine());
    }

    // 여러 메뉴에서 반복되는 문자열 입력 처리를 readLine으로 분리한다.
    // 입력 안내 후 문자열을 입력받아 반환한다.
    static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    // 메뉴 출력 메서드
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

    // 택배 접수
    static void registerParcel() throws ParcelException, SQLException {
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");

        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        String receiverName = readLine("수령인 이름 입력하세요 : ");
        String receiverPhoneNumber = readLine("수령인 연락처를 입력하세요 : ");
        String destination = readLine("배송 지역을 입력하세요 : ");
        LocalDateTime registeredDate = LocalDateTime.now();

        System.out.println("1. 간편 접수(일반 배송, 1kg)");
        System.out.println("2. 상세 접수(배송 종류와 무게 직접 입력)");
        int registerType = readInt("접수 방식: ");

        Parcel parcel;
        if (registerType == 1) {
            parcel = new NormalParcel(
                    trackingNumber,
                    receiverName,
                    receiverPhoneNumber,
                    destination,
                    registeredDate
            );
        } else if (registerType == 2) {
            int weight = readInt("택배 무게를 입력하세요 : ");

            if (weight > Parcel.MAXIMUM_WEIGHT) {
                throw new ParcelException("택배 무게는 20kg을 넘을 수 없습니다.");
            }

            DeliveryType deliveryType = readDeliveryType();
            if (deliveryType == DeliveryType.일반) {
                parcel = new NormalParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.특급) {
                parcel = new ExpressParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.냉장) {
                parcel = new RefrigeratedParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.해외) {
                parcel = new OverseasParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else {
                throw new ParcelException("잘못된 배송 타입입니다.");
            }
        } else {
            throw new ParcelException("접수 방식을 다시 선택해주세요.");
        }

        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        parcelRepository.save(parcel);
        System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);
    }

    // 정해진 배송 종류 중 하나를 입력받은 뒤, 배송 타입으로 변환하여 반환한다.
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

    // 택배 조회
    static void findParcel() throws ParcelException, SQLException {
        String searchTarget = readLine("운송장 번호를 입력하세요 : ");
        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);

        if (parcel == null) {
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        printParcel(parcel);
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("운송장 번호: ").append(parcel.getTrackingNumber()).append('\n');
        stringBuilder.append("수령인: ").append(parcel.getReceiverName()).append('\n');
        stringBuilder.append("연락처: ").append(parcel.getReceiverPhoneNumber()).append('\n');
        stringBuilder.append("배송 지역: ").append(parcel.getDestination()).append('\n');
        stringBuilder.append("배송 종류: ").append(parcel.getDeliveryType()).append('\n');
        stringBuilder.append("무게: ").append(parcel.getWeight()).append("kg\n");
        stringBuilder.append("배송비: ").append(parcel.getFee()).append("원\n");
        stringBuilder.append("상태: ").append(parcel.getParcelStatus()).append('\n');
        stringBuilder.append("접수일: ").append(parcel.getRegisteredDate()).append('\n');
        stringBuilder.append("예상 도착일: ").append(parcel.getExpectedDeliveryDate()).append('\n');
        System.out.println(stringBuilder);
    }

    // 전체 택배 조회
    static void printAllParcels() throws ParcelException, SQLException {
        if (parcelRepository.isEmpty()) {
            throw new ParcelException("접수된 택배가 없습니다.");
        }

        List<Parcel> parcels = parcelRepository.findAll();

        for (Parcel parcel : parcels) {
            System.out.println(
                    parcel.getTrackingNumber() + " / "
                            + parcel.getReceiverName() + " / "
                            + parcel.getDeliveryType() + " / "
                            + parcel.getParcelStatus()
            );
        }
    }

    // 출고/취소 통합 메서드
    // 접수 상태의 택배를 출고 또는 취소 상태로 변경 후 이력 추가하는 메서드
    static void changeParcelStatus(ParcelStatus afterParcelStatus)
            throws ParcelException, SQLException {
        String trackingNumber = readLine(afterParcelStatus + "할 운송장 번호: ");
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);

        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 " + afterParcelStatus + " 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus, LocalDateTime.now());
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // 배송 이력 출력
    static void printHistory() throws ParcelException, SQLException {
        String searchTarget = readLine("조회할 운송장 번호 : ");
        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);

        if (parcel == null) {
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        for (int i = 0; i < parcel.getHistoryCount(); i++) {
            System.out.println(
                    parcel.getTrackingNumber() + "|"
                            + parcel.getHistories()[i].historyChangedDates + "|"
                            + parcel.getHistories()[i].beforeParcelStatus + "|"
                            + parcel.getHistories()[i].afterParcelStatus
            );
        }
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

    // 택배 테이블이 비었는지 확인한다.
    boolean isEmpty() throws SQLException;
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

    // 새로운 택배는 INSERT하고, 기존 택배는 UPDATE한다.
    @Override
    public void save(Parcel parcel) throws SQLException {
        Parcel savedParcel = findByTrackingNumber(parcel.getTrackingNumber());

        if (savedParcel == null) {
            insertParcel(parcel);
        } else {
            updateParcel(parcel);
        }

        insertLatestHistory(parcel);
    }

    // 새로운 택배를 DB에 추가한다.
    private void insertParcel(Parcel parcel) throws SQLException {
        String sql = "INSERT INTO parcels "
                + "(tracking_number, receiver_name, receiver_phone_number, destination, "
                + "weight, delivery_type, fee, status, registered_at, expected_delivery_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parcel.getTrackingNumber());
            statement.setString(2, parcel.getReceiverName());
            statement.setString(3, parcel.getReceiverPhoneNumber());
            statement.setString(4, parcel.getDestination());
            statement.setInt(5, parcel.getWeight());
            statement.setString(6, parcel.getDeliveryType().toString());
            statement.setInt(7, parcel.getFee());
            statement.setString(8, parcel.getParcelStatus().toString());
            statement.setTimestamp(9, Timestamp.valueOf(parcel.getRegisteredDate()));
            statement.setTimestamp(10, Timestamp.valueOf(parcel.getExpectedDeliveryDate()));
            statement.executeUpdate();
        }
    }

    // 기존 택배의 변경된 정보를 DB에 반영한다.
    private void updateParcel(Parcel parcel) throws SQLException {
        String sql = "UPDATE parcels SET "
                + "receiver_name = ?, receiver_phone_number = ?, destination = ?, "
                + "weight = ?, delivery_type = ?, fee = ?, status = ?, "
                + "registered_at = ?, expected_delivery_at = ? "
                + "WHERE tracking_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parcel.getReceiverName());
            statement.setString(2, parcel.getReceiverPhoneNumber());
            statement.setString(3, parcel.getDestination());
            statement.setInt(4, parcel.getWeight());
            statement.setString(5, parcel.getDeliveryType().toString());
            statement.setInt(6, parcel.getFee());
            statement.setString(7, parcel.getParcelStatus().toString());
            statement.setTimestamp(8, Timestamp.valueOf(parcel.getRegisteredDate()));
            statement.setTimestamp(9, Timestamp.valueOf(parcel.getExpectedDeliveryDate()));
            statement.setString(10, parcel.getTrackingNumber());
            statement.executeUpdate();
        }
    }

    // 운송장 번호에 해당하는 택배와 배송 이력을 조회한다.
    @Override
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
    @Override
    public List<Parcel> findAll() throws SQLException {
        List<Parcel> parcels = new ArrayList<>();
        String sql = "SELECT * FROM parcels ORDER BY registered_at";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Parcel parcel = createParcel(resultSet);
                    parcels.add(parcel);
                }
            }
        }

        return parcels;
    }

    // 전체 택배 목록이 비었는지 확인한다.
    @Override
    public boolean isEmpty() throws SQLException {
        return findAll().isEmpty();
    }

    // 가장 최근에 추가된 배송 이력 한 건을 DB에 추가한다.
    private void insertLatestHistory(Parcel parcel) throws SQLException {
        if (parcel.getHistoryCount() == 0) {
            return;
        }

        String sql = "INSERT INTO delivery_histories "
                + "(tracking_number, before_status, after_status, changed_at) VALUES (?, ?, ?, ?)";

        int latestHistoryIndex = parcel.getHistoryCount() - 1;
        DeliveryHistory latestHistory = parcel.getHistories()[latestHistoryIndex];

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parcel.getTrackingNumber());
            statement.setString(2, latestHistory.beforeParcelStatus.toString());
            statement.setString(3, latestHistory.afterParcelStatus.toString());
            statement.setTimestamp(4, Timestamp.valueOf(latestHistory.historyChangedDates));
            statement.executeUpdate();
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
        LocalDateTime registeredDate = resultSet.getTimestamp("registered_at").toLocalDateTime();

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
        parcel.setExpectedDeliveryDate(
                resultSet.getTimestamp("expected_delivery_at").toLocalDateTime()
        );
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
                    parcel.addHistory(
                            ParcelStatus.valueOf(resultSet.getString("before_status")),
                            ParcelStatus.valueOf(resultSet.getString("after_status")),
                            resultSet.getTimestamp("changed_at").toLocalDateTime()
                    );
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
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;
    private static final int JEJU_DELIVERY_FEE = 3000;
    private static final int HEAVY_PARCEL_FEE = 2000;
    private static final int HEAVY_PARCEL_MINIMUM_WEIGHT = 3;
    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private DeliveryType deliveryType;
    private int weight;

    private int fee;
    private ParcelStatus parcelStatus;
    private final LocalDateTime registeredDate;
    private LocalDateTime expectedDeliveryDate;
    private DeliveryHistory[] histories = new DeliveryHistory[20];
    private int historyCount = 0;

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDateTime registeredDate, DeliveryType deliveryType) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
        this.parcelStatus = ParcelStatus.접수;
        this.deliveryType = deliveryType;
    }

    // 택배의 배송 이력 배열에 한 건을 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus,
                    ParcelStatus afterParcelStatus,
                    LocalDateTime historyChangedDate) {
        DeliveryHistory deliveryHistory = new DeliveryHistory(
                beforeParcelStatus,
                afterParcelStatus,
                historyChangedDate
        );

        histories[historyCount] = deliveryHistory;
        historyCount++;
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
    public LocalDateTime getRegisteredDate() {
        return registeredDate;
    }

    // 예상 도착일을 반환한다.
    public LocalDateTime getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    // 예상 도착일을 저장한다.
    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    private boolean setWeight(int weight) {
        if (weight > MAXIMUM_WEIGHT) {
            return false;
        }

        this.weight = weight;
        return true;
    }

    // 배송 이력 배열을 반환한다.
    public DeliveryHistory[] getHistories() {
        return histories;
    }

    // 저장된 배송 이력 개수를 반환한다.
    public int getHistoryCount() {
        return historyCount;
    }

    // 계산되어 저장된 배송비를 반환한다.
    public int getFee() {
        return fee;
    }

    // 자식 클래스가 계산한 배송비를 택배 객체에 저장한다.
    public void setFee(int fee) {
        this.fee = fee;
    }

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int deliveryFee = BASIC_DELIVERY_FEE;
        if (weight >= HEAVY_PARCEL_MINIMUM_WEIGHT) {
            deliveryFee += HEAVY_PARCEL_FEE;
        }
        if (destination.equals("제주")) {
            deliveryFee += JEJU_DELIVERY_FEE;
        }
        return deliveryFee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, LocalDateTime registeredDate) {
        this(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, registeredDate);
    }

    // 일반 배송 택배를 초기화한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.일반);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(3);
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.특급);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(1);
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.냉장);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(1);
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.해외);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(7);
    }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    LocalDateTime historyChangedDates;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeParcelStatus,
                    ParcelStatus afterParcelStatus,
                    LocalDateTime historyChangedDates) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDates = historyChangedDates;
    }
}
