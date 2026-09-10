import java.util.Scanner;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch07Inheritance {
    // 메뉴 입력에 사용하는 스캐너다.
    static Scanner scanner = new Scanner(System.in);

    // CH06에서 만든 택배 객체 배열을 그대로 사용한다.
    static Parcel[] parcels = new Parcel[100];
    static int parcelCount = 0;

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
                    findParcel();
                    break;
                case 3:
                    printAllParcels();
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

    // 배송 종류에 맞는 택배 객체를 만들어 접수한다.
    static void registerParcel() {
        if (parcelCount == parcels.length) {
            System.out.println("더 이상 택배를 접수할 수 없습니다.");
            return;
        }

        String trackingNumber = readLine("운송장 번호: ");
        if (findParcelByTrackingNumber(trackingNumber) != null) {
            System.out.println("이미 사용 중인 운송장 번호입니다.");
            return;
        }

        String receiverName = readLine("수령인 이름: ");
        String receiverPhoneNumber = readLine("수령인 연락처: ");
        String destination = readLine("배송 지역: ");
        String registeredDate = readLine("접수일(예: 2026-09-01): ");
        System.out.println("1. 간편 접수(일반 배송, 1kg)");
        System.out.println("2. 상세 접수(배송 종류와 무게 직접 입력)");
        int registerType = readInt("접수 방식: ");
        Parcel parcel;

        if (registerType == 1) {
            parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, registeredDate);
        } else if (registerType == 2) {
            int weight = readInt("무게(kg): ");
            if (weight > Parcel.MAXIMUM_WEIGHT) {
                System.out.println("택배 무게는 20kg을 넘을 수 없습니다.");
                return;
            }

            String deliveryType = readDeliveryType();
            parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, weight, deliveryType, registeredDate);
        } else {
            System.out.println("접수 방식을 다시 선택하세요.");
            return;
        }

        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        parcels[parcelCount] = parcel;
        parcelCount++;

        System.out.println("택배가 접수되었습니다. 배송비: " + parcel.calculateFee() + "원");
    }

    // ========== CH07 변경 ==========
    // 배송 종류 enum과 배송비 조건문을 자식 클래스 선택으로 바꾼다.
    // =================================
    static Parcel createParcel(String trackingNumber, String receiverName,
                               String receiverPhoneNumber, String destination,
                               int weight, String deliveryType, String registeredDate) {
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

    // 운송장 번호로 택배 한 건을 조회한다.
    static void findParcel() {
        Parcel parcel = findParcelByTrackingNumber(readLine("운송장 번호: "));

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        printParcel(parcel);
    }

    // 접수된 모든 택배의 요약 정보를 출력한다.
    static void printAllParcels() {
        if (parcelCount == 0) {
            System.out.println("접수된 택배가 없습니다.");
            return;
        }

        for (int index = 0; index < parcelCount; index++) {
            Parcel parcel = parcels[index];
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 출고 또는 취소 상태로 바꾸고 이력을 남긴다.
    static void changeStatus(ParcelStatus afterParcelStatus, String dateMessage) {
        String trackingNumber = readLine(afterParcelStatus + "할 운송장 번호: ");
        Parcel parcel = findParcelByTrackingNumber(trackingNumber);

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 처리할 수 있습니다.");
            return;
        }

        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus, readLine(dateMessage));
        parcel.setParcelStatus(afterParcelStatus);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // 특정 택배 안에 저장된 배송 이력을 출력한다.
    static void printHistory() {
        Parcel parcel = findParcelByTrackingNumber(readLine("운송장 번호: "));

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        for (int index = 0; index < parcel.getHistoryCount(); index++) {
            DeliveryHistory history = parcel.getHistories()[index];
            System.out.println(history.getHistoryChangedDate() + " / " + history.getBeforeParcelStatus()
                    + " → " + history.getAfterParcelStatus());
        }
    }

    // 운송장 번호가 같은 택배 객체를 찾는다.
    static Parcel findParcelByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcelCount; index++) {
            if (parcels[index].getTrackingNumber().equals(trackingNumber)) {
                return parcels[index];
            }
        }

        return null;
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        System.out.println("운송장 번호: " + parcel.getTrackingNumber());
        System.out.println("수령인: " + parcel.getReceiverName());
        System.out.println("연락처: " + parcel.getReceiverPhoneNumber());
        System.out.println("배송 지역: " + parcel.getDestination());
        System.out.println("배송 종류: " + parcel.getDeliveryType());
        System.out.println("무게: " + parcel.getWeight() + "kg");
        System.out.println("배송비: " + parcel.calculateFee() + "원");
        System.out.println("상태: " + parcel.getParcelStatus());
        System.out.println("접수일: " + parcel.getRegisteredDate());
        System.out.println("예상 도착: " + parcel.getExpectedDeliveryDays() + "일 후");
    }

    // 정해진 배송 종류 중 하나를 입력받는다.
    static String readDeliveryType() {
        while (true) {
            String deliveryType = readLine("배송 종류(일반/특급/냉장/해외): ");

            if (deliveryType.equals("일반")) {
                return deliveryType;
            }

            if (deliveryType.equals("특급")) {
                return deliveryType;
            }

            if (deliveryType.equals("냉장")) {
                return deliveryType;
            }

            if (deliveryType.equals("해외")) {
                return deliveryType;
            }

            System.out.println("배송 종류를 다시 입력하세요.");
        }
    }

    // 정수 입력을 받을 때까지 반복한다.
    static int readInt(String message) {
        System.out.print(message);
        return Integer.parseInt(scanner.nextLine());
    }

    // 한 줄 문자열을 입력받는다.
    static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }
}

// 배송 종류가 공유하는 기본 정보와 기능을 가진 부모 클래스다.
// ========== CH07 변경 ==========
// 공통 정보는 부모 클래스에 두고, 배송 종류별 계산은 자식 클래스에 맡긴다.
// =================================
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
    private ParcelStatus parcelStatus;
    private final String registeredDate;
    private DeliveryHistory[] histories = new DeliveryHistory[20];
    private int historyCount = 0;

    // 모든 배송 종류가 공통으로 사용하는 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, String registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
        this.parcelStatus = ParcelStatus.접수;
    }

    // 이 택배의 상태 변경 이력을 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, String historyChangedDate) {
        histories[historyCount] = new DeliveryHistory(beforeParcelStatus, afterParcelStatus, historyChangedDate);
        historyCount++;
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
    String getRegisteredDate() {
        return registeredDate;
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    boolean setWeight(int weight) {
        if (weight > MAXIMUM_WEIGHT) {
            return false;
        }

        this.weight = weight;
        return true;
    }

    // 배송 이력 배열을 반환한다.
    DeliveryHistory[] getHistories() {
        return histories;
    }

    // 저장된 배송 이력 개수를 반환한다.
    int getHistoryCount() {
        return historyCount;
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 화면에 표시할 배송 종류 이름을 자식 클래스에 맡긴다.
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

// 일반 배송의 계산 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, String registeredDate) {
        this(trackingNumber, receiverName, receiverPhoneNumber, destination, 1, registeredDate);
    }

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
    private ParcelStatus beforeParcelStatus;
    private ParcelStatus afterParcelStatus;
    private String historyChangedDate;

    // 배송 이력 한 건을 초기화한다.
    // 변경 전 상태를 반환한다.
    ParcelStatus getBeforeParcelStatus() {
        return beforeParcelStatus;
    }

    // 변경 후 상태를 반환한다.
    ParcelStatus getAfterParcelStatus() {
        return afterParcelStatus;
    }

    // 변경 날짜를 반환한다.
    String getHistoryChangedDate() {
        return historyChangedDate;
    }

    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, String historyChangedDate) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDate = historyChangedDate;
    }
}
