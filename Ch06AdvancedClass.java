import java.util.Scanner;

// 택배 접수 때 선택할 수 있는 배송 종류다.
enum DeliveryType {
    일반,
    특급,
    냉장,
    해외
}

// 택배의 현재 배송 상태와 이력에 사용할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

// ========== CH06 고급 클래스 변경 ==========
// Ch06Methods의 메서드 구조는 유지한다.
// 생성자 오버로딩, final, getter/setter와 무게 검증만 추가한다.
// =================================
public class Ch06AdvancedClass {
    static Scanner scanner = new Scanner(System.in);
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
                    changeParcelStatus(ParcelStatus.출고, "출고일: ");
                    break;
                case 5:
                    changeParcelStatus(ParcelStatus.취소, "취소일: ");
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

    // 간편 접수 또는 상세 접수 방식으로 택배를 만든다.
    static void registerParcel() {
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
            // 간편 접수용 생성자를 사용한다.
            parcel = new Parcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, registeredDate);
        } else if (registerType == 2) {
            int weight = readInt("무게(kg): ");
            if (weight > Parcel.MAXIMUM_WEIGHT) {
                System.out.println("택배 무게는 20kg을 넘을 수 없습니다.");
                return;
            }

            DeliveryType deliveryType = readDeliveryType();
            // 상세 접수용 생성자를 사용한다.
            parcel = new Parcel(trackingNumber, receiverName, receiverPhoneNumber,
                    destination, deliveryType, weight, registeredDate);
        } else {
            System.out.println("접수 방식을 다시 선택하세요.");
            return;
        }

        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        parcels[parcelCount] = parcel;
        parcelCount++;
        System.out.println("택배가 접수되었습니다. 배송비: " + parcel.getDeliveryFee() + "원");
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

    // 모든 택배의 요약 정보를 출력한다.
    static void printAllParcels() {
        for (int index = 0; index < parcelCount; index++) {
            Parcel parcel = parcels[index];
            System.out.println(parcel.getTrackingNumber() + " / " + parcel.getReceiverName()
                    + " / " + parcel.getDeliveryType() + " / " + parcel.getParcelStatus());
        }
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    static void changeParcelStatus(ParcelStatus afterParcelStatus, String dateMessage) {
        Parcel parcel = findParcelByTrackingNumber(readLine(afterParcelStatus + "할 운송장 번호: "));
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

    // 특정 택배의 배송 이력을 출력한다.
    static void printHistory() {
        Parcel parcel = findParcelByTrackingNumber(readLine("운송장 번호: "));
        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }
        for (int index = 0; index < parcel.getHistoryCount(); index++) {
            DeliveryHistory history = parcel.getHistories()[index];
            System.out.println(history.getHistoryChangedDate() + " / "
                    + history.getBeforeParcelStatus() + " → " + history.getAfterParcelStatus());
        }
    }

    // 운송장 번호가 같은 택배를 찾는다.
    static Parcel findParcelByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcelCount; index++) {
            if (parcels[index].getTrackingNumber().equals(trackingNumber)) {
                return parcels[index];
            }
        }
        return null;
    }

    // 택배 한 건의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        System.out.println("운송장 번호: " + parcel.getTrackingNumber());
        System.out.println("수령인: " + parcel.getReceiverName());
        System.out.println("연락처: " + parcel.getReceiverPhoneNumber());
        System.out.println("배송 지역: " + parcel.getDestination());
        System.out.println("배송 종류: " + parcel.getDeliveryType());
        System.out.println("무게: " + parcel.getWeight() + "kg");
        System.out.println("배송비: " + parcel.getDeliveryFee() + "원");
        System.out.println("상태: " + parcel.getParcelStatus());
        System.out.println("접수일: " + parcel.getRegisteredDate());
        System.out.println("예상 도착: " + parcel.getExpectedDeliveryDates() + "일 후");
    }

    // 배송 종류를 입력받는다.
    static DeliveryType readDeliveryType() {
        while (true) {
            String deliveryTypeString = readLine("배송 종류(일반/특급/냉장/해외): ");
            if (deliveryTypeString.equals("일반")) return DeliveryType.일반;
            if (deliveryTypeString.equals("특급")) return DeliveryType.특급;
            if (deliveryTypeString.equals("냉장")) return DeliveryType.냉장;
            if (deliveryTypeString.equals("해외")) return DeliveryType.해외;
            System.out.println("배송 종류를 다시 입력하세요.");
        }
    }

    // 정수를 입력받는다.
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

// 택배 한 건의 정보와 규칙을 관리하는 클래스다.
class Parcel {
    // 택배 무게 제한은 프로그램 전체에서 바꾸지 않는 규칙이다.
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;

    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private final DeliveryType deliveryType;
    private int weight;
    private int deliveryFee;
    private ParcelStatus parcelStatus;
    private final String registeredDate;
    private int expectedDeliveryDates;
    private DeliveryHistory[] histories = new DeliveryHistory[20];
    private int historyCount = 0;

    // 간편 접수는 일반 배송과 1kg을 기본값으로 사용한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, String registeredDate) {
        this(trackingNumber, receiverName, receiverPhoneNumber, destination,
                DeliveryType.일반, 1, registeredDate);
    }

    // 상세 접수는 배송 종류와 무게를 직접 받는다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, DeliveryType deliveryType, int weight, String registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.deliveryType = deliveryType;
        this.registeredDate = registeredDate;
        this.parcelStatus = ParcelStatus.접수;
        setWeight(weight);
        calculateDeliveryFee();
        calculateExpectedDeliveryDates();
    }

    // 택배 무게를 저장한다. 20kg을 넘으면 택배를 만들 수 없다.
    void setWeight(int weight) {
        this.weight = weight;
    }

    // 상태 변경 이력을 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, String historyChangedDate) {
        histories[historyCount] = new DeliveryHistory(beforeParcelStatus, afterParcelStatus, historyChangedDate);
        historyCount++;
    }

    // 무게, 지역, 배송 종류에 따라 배송비를 계산한다.
    private void calculateDeliveryFee() {
        deliveryFee = BASIC_DELIVERY_FEE;
        if (weight >= 3) {
            deliveryFee += 2000;
        }

        if (destination.equals("제주")) {
            deliveryFee += 3000;
        }

        if (deliveryType == DeliveryType.특급) {
            deliveryFee += 2000;
        } else if (deliveryType == DeliveryType.냉장) {
            deliveryFee += 4000;
        } else if (deliveryType == DeliveryType.해외) {
            deliveryFee += 15000;
        }
    }

    // 배송 종류에 따라 예상 배송 소요일을 계산한다.
    private void calculateExpectedDeliveryDates() {
        expectedDeliveryDates = 3;
        if (deliveryType == DeliveryType.특급) {
            expectedDeliveryDates = 1;
        } else if (deliveryType == DeliveryType.냉장) {
            expectedDeliveryDates = 1;
        } else if (deliveryType == DeliveryType.해외) {
            expectedDeliveryDates = 7;
        }
    }

    String getTrackingNumber() {
        return trackingNumber;
    }
    String getReceiverName() {
        return receiverName;
    }
    String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }
    String getDestination() {
        return destination;
    }
    DeliveryType getDeliveryType() {
        return deliveryType;
    }
    int getWeight() {
        return weight;
    }
    int getDeliveryFee() {
        return deliveryFee;
    }
    ParcelStatus getParcelStatus() {
        return parcelStatus;
    }
    String getRegisteredDate() {
        return registeredDate;
    }
    int getExpectedDeliveryDates() {
        return expectedDeliveryDates;
    }
    DeliveryHistory[] getHistories() {
        return histories;
    }
    int getHistoryCount() {
        return historyCount;
    }
    void setParcelStatus(ParcelStatus parcelStatus) {
        this.parcelStatus = parcelStatus;
    }
}

// 택배 상태가 바뀐 기록 한 건을 보관하는 클래스다.
class DeliveryHistory {
    private ParcelStatus beforeParcelStatus;
    private ParcelStatus afterParcelStatus;
    private String historyChangedDate;

    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, String historyChangedDate) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDate = historyChangedDate;
    }

    ParcelStatus getBeforeParcelStatus() {
        return beforeParcelStatus;
    }
    ParcelStatus getAfterParcelStatus() {
        return afterParcelStatus;
    }
    String getHistoryChangedDate() {
        return historyChangedDate;
    }
}
