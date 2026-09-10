import java.util.Scanner;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch08Repository {
    // 메뉴 입력에 사용하는 스캐너다.
    static Scanner scanner = new Scanner(System.in);

    // ========== CH08 변경 ==========
    // Main은 메뉴와 입력만 담당한다.
    // =================================
    static ParcelService parcelService = new ParcelService(new MemoryParcelRepository());

    // 프로그램 메뉴를 반복해서 실행한다.
    public static void main(String[] args) {
        while (true) {
            printMenu();
            int menu = readInt("메뉴 선택: ");

            switch (menu) {
                case 1:
                    registerParcel();
                    break;
                case 2:
                    printParcel();
                    break;
                case 3:
                    parcelService.printAllParcels();
                    break;
                case 4:
                    changeStatus(ParcelStatus.출고, "출고일: ");
                    break;
                case 5:
                    changeStatus(ParcelStatus.취소, "취소일: ");
                    break;
                case 6:
                    printHistory();
                    break;
                case 0:
                    System.out.println("프로그램을 종료합니다.");
                    return;
                default:
                    System.out.println("메뉴 번호를 다시 입력하세요.");
            }
        }
    }

    // 사용자에게 메뉴를 보여 준다.
    static void printMenu() {
        System.out.println("\n========== 당일배송 물류센터 ==========");
        System.out.println("1. 택배 접수");
        System.out.println("2. 운송장 번호로 조회");
        System.out.println("3. 전체 택배 조회");
        System.out.println("4. 출고 처리");
        System.out.println("5. 배송 취소");
        System.out.println("6. 배송 이력 조회");
        System.out.println("0. 종료");
        System.out.println("=====================================");
    }

    // 입력받은 정보로 택배 접수 업무를 요청한다.
    static void registerParcel() {
        String trackingNumber = readLine("운송장 번호: ");
        String receiverName = readLine("수령인 이름: ");
        String receiverPhoneNumber = readLine("수령인 연락처: ");
        String destination = readLine("배송 지역: ");
        int weight = readInt("무게(kg): ");
        String deliveryType = readDeliveryType();
        String registeredDate = readLine("접수일(예: 2026-09-01): ");

        parcelService.register(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                weight,
                deliveryType,
                registeredDate
        );
    }

    // 운송장 번호로 택배 상세 정보를 출력한다.
    static void printParcel() {
        parcelService.printParcel(readLine("운송장 번호: "));
    }

    // 출고 또는 취소 상태 변경을 요청한다.
    static void changeStatus(ParcelStatus afterStatus, String dateMessage) {
        String trackingNumber = readLine(afterStatus + "할 운송장 번호: ");
        String changedDate = readLine(dateMessage);
        parcelService.changeStatus(trackingNumber, afterStatus, changedDate);
    }

    // 운송장 번호의 배송 이력을 출력한다.
    static void printHistory() {
        parcelService.printHistory(readLine("운송장 번호: "));
    }

    // 정해진 배송 종류 중 하나를 입력받는다.
    static String readDeliveryType() {
        while (true) {
            String deliveryType = readLine("배송 종류(일반/특급/냉장/해외): ");
            if (deliveryType.equals("일반") || deliveryType.equals("특급")
                    || deliveryType.equals("냉장") || deliveryType.equals("해외")) {
                return deliveryType;
            }
            System.out.println("배송 종류를 다시 입력하세요.");
        }
    }

    // 정수 입력을 받을 때까지 반복한다.
    static int readInt(String message) {
        while (true) {
            try {
                return Integer.parseInt(readLine(message));
            } catch (NumberFormatException exception) {
                System.out.println("숫자를 입력하세요.");
            }
        }
    }

    // 한 줄 문자열을 입력받는다.
    static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }
}

// 택배 업무 규칙을 처리하는 클래스다.
class ParcelService {
    // ========== CH08 변경 ==========
    // 저장 방식은 인터페이스 타입으로 받는다.
    // =================================
    private ParcelRepository parcelRepository;

    // 택배 업무에 필요한 저장소를 전달받는다.
    ParcelService(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    // 입력받은 정보로 배송 종류에 맞는 택배를 접수한다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType, String registeredDate) {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            System.out.println("이미 사용 중인 운송장 번호입니다.");
            return;
        }

        Parcel parcel = createParcel(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                weight,
                deliveryType,
                registeredDate
        );

        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        parcelRepository.save(parcel);
        System.out.println("택배가 접수되었습니다. 배송비: " + parcel.calculateFee() + "원");
    }

    // 배송 종류에 맞는 자식 택배 객체를 만든다.
    Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination, int weight, String deliveryType, String registeredDate) {
        if (deliveryType.equals("특급")) {
            return new ExpressParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        if (deliveryType.equals("냉장")) {
            return new RefrigeratedParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        if (deliveryType.equals("해외")) {
            return new OverseasParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, registeredDate);
        }
        return new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
    }

    // 운송장 번호에 해당하는 택배를 상세 출력한다.
    void printParcel(String trackingNumber) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        System.out.println("운송장 번호: " + parcel.trackingNumber);
        System.out.println("수령인: " + parcel.receiverName);
        System.out.println("연락처: " + parcel.receiverPhoneNumber);
        System.out.println("배송 지역: " + parcel.destination);
        System.out.println("배송 종류: " + parcel.getDeliveryType());
        System.out.println("무게: " + parcel.weight + "kg");
        System.out.println("배송비: " + parcel.calculateFee() + "원");
        System.out.println("상태: " + parcel.status);
        System.out.println("접수일: " + parcel.registeredDate);
        System.out.println("예상 도착: " + parcel.getExpectedDeliveryDays() + "일 후");
    }

    // 저장소에 있는 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() {
        Parcel[] parcels = parcelRepository.findAll();
        int parcelCount = parcelRepository.size();

        if (parcelCount == 0) {
            System.out.println("접수된 택배가 없습니다.");
            return;
        }

        for (int index = 0; index < parcelCount; index++) {
            Parcel parcel = parcels[index];
            System.out.println(parcel.trackingNumber + " / " + parcel.receiverName
                    + " / " + parcel.getDeliveryType() + " / " + parcel.status);
        }
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, ParcelStatus afterStatus, String changedDate) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }
        if (parcel.status != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 처리할 수 있습니다.");
            return;
        }

        parcel.addHistory(parcel.status, afterStatus, changedDate);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
        System.out.println(afterStatus + " 처리했습니다.");
    }

    // 특정 택배의 배송 이력을 출력한다.
    void printHistory(String trackingNumber) {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        for (int index = 0; index < parcel.historyCount; index++) {
            DeliveryHistory history = parcel.histories[index];
            System.out.println(history.changedDate + " / " + history.beforeStatus
                    + " → " + history.afterStatus);
        }
    }
}

// 택배를 저장하고 찾는 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배를 저장하거나 기존 택배 정보를 갱신한다.
    void save(Parcel parcel);

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber);

    // 저장된 택배 배열을 반환한다.
    Parcel[] findAll();

    // 현재 저장된 택배 개수를 반환한다.
    int size();
}

// 택배를 배열에 저장하는 Repository 구현체다.
class MemoryParcelRepository implements ParcelRepository {
    private Parcel[] parcels = new Parcel[100];
    private int count = 0;

    // 배열에 새 택배를 저장하거나 같은 객체는 그대로 둔다.
    public void save(Parcel parcel) {
        if (findByTrackingNumber(parcel.trackingNumber) == null) {
            parcels[count] = parcel;
            count++;
        }
    }

    // 배열에서 운송장 번호가 같은 택배를 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < count; index++) {
            if (parcels[index].trackingNumber.equals(trackingNumber)) {
                return parcels[index];
            }
        }
        return null;
    }

    // 저장된 배열 자체를 반환한다.
    public Parcel[] findAll() {
        return parcels;
    }

    // 현재 저장된 택배 개수를 반환한다.
    public int size() {
        return count;
    }
}

// 배송 종류가 공유하는 기본 정보와 기능을 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    ParcelStatus status;
    String registeredDate;
    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;

    // 모든 배송 종류가 공통으로 사용하는 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, String registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.weight = weight;
        this.registeredDate = registeredDate;
        this.status = ParcelStatus.접수;
    }

    // 이 택배의 상태 변경 이력을 추가한다.
    void addHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus, String changedDate) {
        histories[historyCount] = new DeliveryHistory(beforeStatus, afterStatus, changedDate);
        historyCount++;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 화면에 표시할 배송 종류 이름을 자식 클래스에 맡긴다.
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

// 일반 배송의 계산 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 일반 배송 택배를 초기화한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, int weight, String registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }

    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }

    // 일반 배송의 예상 도착 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 3;
    }

    // 일반 배송 이름을 반환한다.
    String getDeliveryType() {
        return "일반";
    }
}

// 특급 배송의 계산 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }

    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }

    // 특급 배송의 예상 도착 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }

    // 특급 배송 이름을 반환한다.
    String getDeliveryType() {
        return "특급";
    }
}

// 냉장 배송의 계산 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, String registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }

    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }

    // 냉장 배송의 예상 도착 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }

    // 냉장 배송 이름을 반환한다.
    String getDeliveryType() {
        return "냉장";
    }
}

// 해외 배송의 계산 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, String registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }

    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }

    // 해외 배송의 예상 도착 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 7;
    }

    // 해외 배송 이름을 반환한다.
    String getDeliveryType() {
        return "해외";
    }
}

// 택배 상태가 바뀐 시점을 기록하는 클래스다.
class DeliveryHistory {
    ParcelStatus beforeStatus;
    ParcelStatus afterStatus;
    String changedDate;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeStatus, ParcelStatus afterStatus, String changedDate) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedDate = changedDate;
    }
}
