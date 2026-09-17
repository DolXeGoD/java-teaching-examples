import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class Ch15Collections {
    static Scanner scanner = new Scanner(System.in);
    static ParcelRepository parcelRepository = new MemoryParcelRepository();

    // 컬렉션으로 바뀐 저장소를 실행해 보는 예시다.
    public static void main(String[] args) {
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
                        System.out.println("없는 메뉴입니다. 다시 선택해주세요.");
                        break;
                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
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

    // 택배를 접수한다.
    static void registerParcel() throws ParcelException {
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");

        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        String receiverName = readLine("수령인 이름 입력하세요 : ");
        String receiverPhoneNumber = readLine("수령인 연락처를 입력하세요 : ");
        String destination = readLine("배송 지역을 입력하세요 : ");
        LocalDate registeredDate = LocalDate.now();

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

        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
        System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);
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

    // 운송장 번호로 택배를 조회한다.
    static void findParcel() throws ParcelException {
        String searchTarget = readLine("운송장 번호를 입력하세요 : ");
        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);

        if (parcel == null) {
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        printParcel(parcel);
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
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
        System.out.println(detail);
    }

    // ========== CH15 변경 ==========
    // List가 반환한 택배 목록을 향상된 for문으로 순회한다.
    // =================================
    // 전체 택배를 조회한다.
    static void printAllParcels() throws ParcelException {
        if (parcelRepository.findAll().isEmpty()) {
            throw new ParcelException("접수된 택배가 없습니다.");
        }

        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 변경한다.
    static void changeParcelStatus(ParcelStatus afterParcelStatus) throws ParcelException {
        String trackingNumber = readLine(afterParcelStatus + "할 운송장 번호: ");
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);

        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 " + afterParcelStatus + " 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus);
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // ========== CH15 변경 ==========
    // List에 담긴 배송 이력을 향상된 for문으로 순회한다.
    // =================================
    // 배송 이력을 출력한다.
    static void printHistory() throws ParcelException {
        String searchTarget = readLine("조회할 운송장 번호 : ");
        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);

        if (parcel == null) {
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder historyText = new StringBuilder();

        for (DeliveryHistory history : parcel.getHistories()) {
            historyText.append(parcel.getTrackingNumber()).append("|");
            historyText.append(history.getChangedAt().format(formatter)).append("|");
            historyText.append(history.getBeforeParcelStatus()).append("|");
            historyText.append(history.getAfterParcelStatus()).append('\n');
        }

        System.out.print(historyText);
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) {
        super(message);
    }
}

// 택배 저장과 조회 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배를 저장하거나 갱신한다.
    void save(Parcel parcel);

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber);

    // 저장된 모든 택배를 반환한다.
    List<Parcel> findAll();
}

// ========== CH15 변경 ==========
// 배열 저장소를 List와 Map으로 바꾼다.
// =================================
class MemoryParcelRepository implements ParcelRepository {
    private List<Parcel> parcelList = new ArrayList<>();
    private Map<String, Parcel> parcelMap = new HashMap<>();

    // 새 택배를 목록과 운송장 번호 Map에 함께 저장한다.
    public void save(Parcel parcel) {
        if (!parcelMap.containsKey(parcel.getTrackingNumber())) {
            parcelList.add(parcel);
        }

        parcelMap.put(parcel.getTrackingNumber(), parcel);
    }

    // Map에서 운송장 번호로 택배를 바로 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        return parcelMap.get(trackingNumber);
    }

    // 전체 택배 목록을 반환한다.
    public List<Parcel> findAll() {
        return parcelList;
    }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;
    private static final int HEAVY_PARCEL_FEE = 2000;
    private static final int JEJU_DELIVERY_FEE = 3000;
    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private DeliveryType deliveryType;
    private int weight;

    private int fee;
    private ParcelStatus parcelStatus;
    private final LocalDate registeredDate;
    private LocalDate expectedDeliveryDate;

    // ========== CH15 변경 ==========
    // 배송 이력 배열을 ArrayList로 변경한다.
    // =================================
    private List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate, DeliveryType deliveryType) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
        this.parcelStatus = ParcelStatus.접수;
        this.deliveryType = deliveryType;
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
    private boolean setWeight(int weight) {
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
                weight, registeredDate, DeliveryType.일반);
        setFee(calculateFee());
        setExpectedDeliveryDate(registeredDate.plusDays(calculateExpectedDeliveryDays()));
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    int calculateExpectedDeliveryDays() {
        return 3;
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.특급);
        setFee(calculateFee());
        setExpectedDeliveryDate(registeredDate.plusDays(calculateExpectedDeliveryDays()));
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    int calculateExpectedDeliveryDays() {
        return 1;
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.냉장);
        setFee(calculateFee());
        setExpectedDeliveryDate(registeredDate.plusDays(calculateExpectedDeliveryDays()));
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    int calculateExpectedDeliveryDays() {
        return 1;
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.해외);
        setFee(calculateFee());
        setExpectedDeliveryDate(registeredDate.plusDays(calculateExpectedDeliveryDays()));
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    int calculateExpectedDeliveryDays() {
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
