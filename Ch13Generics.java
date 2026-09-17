import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public class Ch13Generics {
    static Scanner scanner = new Scanner(System.in);
    static ParcelRepository parcelRepository = new MemoryParcelRepository();

    // 제네릭 저장소가 택배와 배송 이력에 모두 쓰이는지 확인한다.
    public static void main(String[] args) {
        while (true) {
            try {
                printMenu();
                int menu = readInt("메뉴 선택 : ");

                switch (menu) {
                    case 0:
                        if (parcelRepository.size() > 0) {
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
    static void registerParcel() throws ParcelException {
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
    static void findParcel() throws ParcelException {
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");
        System.out.println(getDetail(trackingNumber));
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 변경한다.
    static void changeParcelStatus(ParcelStatus afterParcelStatus)
            throws ParcelException {
        String trackingNumber =
                readLine(afterParcelStatus + "할 운송장 번호를 입력하세요 : ");

        changeStatus(trackingNumber, afterParcelStatus);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // 운송장 번호를 입력받아 해당 택배의 배송 이력을 출력한다.
    static void printHistory() throws ParcelException {
        String trackingNumber = readLine("조회할 운송장 번호 : ");
        System.out.print(getHistoryText(trackingNumber));
    }

    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    static void registerSimple(String trackingNumber, String receiverName, String receiverPhoneNumber,
                               String destination) throws ParcelException {
        register(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, DeliveryType.일반);
    }

    // 새 택배를 접수하고 최초 이력을 남긴다.
    static void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                         String destination, int weight, DeliveryType deliveryType) throws ParcelException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        if (!parcelRepository.hasSpace()) {
            throw new ParcelException("더 이상 택배를 접수할 수 없습니다.");
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
    static void changeStatus(String trackingNumber, ParcelStatus afterParcelStatus) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus);
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
    }

    // 운송장 번호에 해당하는 택배 정보를 문자열로 정리해 반환한다.
    static String getDetail(String trackingNumber) throws ParcelException {
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

    // 택배 한 건의 배송 이력을 문자열로 정리해 반환한다.
    static String getHistoryText(String trackingNumber) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        StringBuilder historyText = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int index = 0; index < parcel.getHistories().size(); index++) {
            DeliveryHistory history = parcel.getHistories().get(index);
            historyText.append(history.getChangedAt().format(formatter));
            historyText.append(" / ").append(history.getBeforeParcelStatus());
            historyText.append(" → ").append(history.getAfterParcelStatus()).append('\n');
        }

        return historyText.toString();
    }

    // 특정 택배의 배송 이력을 출력한다.
    static void printHistory(String trackingNumber) throws ParcelException {
        System.out.print(getHistoryText(trackingNumber));
    }

    // 저장소에 있는 모든 택배의 요약 정보를 출력한다.
    static void printAllParcels() {
        Parcel[] parcels = parcelRepository.findAll();

        for (int index = 0; index < parcelRepository.size(); index++) {
            Parcel parcel = parcels[index];
        System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    static Parcel findParcel(String trackingNumber) throws ParcelException {
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

// ========== CH13 변경 ==========
// 자료형만 다른 배열 관리 코드를 제네릭 저장소로 묶는다.
// =================================
class MemoryStorage<T> {
    private T[] data;
    private int count = 0;

    // 어떤 자료형 배열을 사용할지 받아 저장소를 만든다.
    MemoryStorage(T[] data) {
        this.data = data;
    }

    // 저장소 끝에 데이터를 추가한다.
    void add(T item) {
        if (!hasSpace()) {
            return;
        }

        data[count] = item;
        count++;
    }

    // 지정한 위치의 데이터를 반환한다.
    T get(int index) {
        return data[index];
    }

    // 현재 저장된 데이터 개수를 반환한다.
    int size() {
        return count;
    }

    // 저장소에 새 데이터를 추가할 공간이 남아 있는지 확인한다.
    boolean hasSpace() {
        return count < data.length;
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

    // 저장된 택배 배열을 반환한다.
    Parcel[] findAll();

    // 현재 저장된 택배 개수를 반환한다.
    int size();

    // 새 택배를 저장할 공간이 남아 있는지 확인한다.
    boolean hasSpace();
}

// ========== CH13 변경 ==========
// 택배 배열 대신 MemoryStorage<Parcel>을 사용한다.
// =================================
class MemoryParcelRepository implements ParcelRepository {
    private MemoryStorage<Parcel> parcels = new MemoryStorage<>(new Parcel[100]);

    // 새 택배만 저장소에 추가한다.
    public void save(Parcel parcel) {
    if (findByTrackingNumber(parcel.getTrackingNumber()) == null) {
            parcels.add(parcel);
        }
    }

    // 저장소에서 운송장 번호가 같은 택배를 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcels.size(); index++) {
            Parcel parcel = parcels.get(index);
        if (parcel.getTrackingNumber().equals(trackingNumber)) {
                return parcel;
            }
        }
        return null;
    }

    // 저장된 택배를 배열로 반환한다.
    public Parcel[] findAll() {
        Parcel[] foundParcels = new Parcel[parcels.size()];

        for (int index = 0; index < parcels.size(); index++) {
            foundParcels[index] = parcels.get(index);
        }

        return foundParcels;
    }

    // 현재 저장된 택배 개수를 반환한다.
    public int size() {
        return parcels.size();
    }

    // 저장소에 새 택배를 추가할 공간이 남아 있는지 확인한다.
    public boolean hasSpace() {
        return parcels.hasSpace();
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

    // ========== CH13 변경 ==========
    // 이력 배열을 MemoryStorage<DeliveryHistory>로 바꾼다.
    // =================================
    private MemoryStorage<DeliveryHistory> histories = new MemoryStorage<>(new DeliveryHistory[20]);

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

    // 상태 변경 이력을 제네릭 저장소에 추가한다.
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

    // 배송 이력 저장소를 반환한다.
    public MemoryStorage<DeliveryHistory> getHistories() {
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
