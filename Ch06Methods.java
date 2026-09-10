import java.util.Scanner;

// 택배 접수 때 선택할 수 있는 배송 종류다.
enum DeliveryType {
    일반,
    특급,
    냉장,
    해외
}

// 택배가 가질 수 있는 현재 배송 상태다.
enum ParcelStatus {
    접수,
    출고,
    취소
}

// ========== CH06 메서드 변경 ==========
// Ch06Classes에서 main에 작성한 기능별 코드를 static 메서드로 분리한다.
// =================================
public class Ch06Methods {
    // 메뉴 입력에 사용하는 스캐너다.
    static Scanner scanner = new Scanner(System.in);

    // ========== CH06 변경 ==========
    // 병렬 배열 대신 택배 객체 배열을 사용한다.
    // =================================
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
                    shipParcel();
                    break;
                case 5:
                    cancelParcel();
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

    // 택배 객체를 만들어 배열에 저장한다.
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
        int weight = readInt("무게(kg): ");
        DeliveryType deliveryType = readDeliveryType();
        String registeredDate = readLine("접수일(예: 2026-09-01): ");

        Parcel parcel = new Parcel(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                deliveryType,
                weight,
                registeredDate
        );

        parcel.fee = calculateFee(parcel);
        parcel.expectedDeliveryDays = calculateExpectedDeliveryDays(parcel.deliveryType);
        parcel.addHistory("없음", "접수", registeredDate);

        parcels[parcelCount] = parcel;
        parcelCount++;

        System.out.println("택배가 접수되었습니다. 배송비: " + parcel.fee + "원");
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
            System.out.println(
                    parcel.trackingNumber + " / "
                            + parcel.receiverName + " / "
                            + parcel.deliveryType + " / "
                            + parcel.status
            );
        }
    }

    // 접수 상태의 택배를 출고 상태로 바꾼다.
    static void shipParcel() {
        Parcel parcel = findParcelByTrackingNumber(readLine("출고할 운송장 번호: "));

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        if (parcel.status != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
            return;
        }

        parcel.addHistory("접수", "출고", readLine("출고일: "));
        parcel.status = ParcelStatus.출고;
        System.out.println("출고 처리했습니다.");
    }

    // 접수 상태의 택배를 취소 상태로 바꾼다.
    static void cancelParcel() {
        Parcel parcel = findParcelByTrackingNumber(readLine("취소할 운송장 번호: "));

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        if (parcel.status != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
            return;
        }

        parcel.addHistory("접수", "취소", readLine("취소일: "));
        parcel.status = ParcelStatus.취소;
        System.out.println("배송을 취소했습니다.");
    }

    // 특정 택배 안에 저장된 배송 이력을 출력한다.
    static void printHistory() {
        Parcel parcel = findParcelByTrackingNumber(readLine("운송장 번호: "));

        if (parcel == null) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        for (int index = 0; index < parcel.historyCount; index++) {
            DeliveryHistory history = parcel.histories[index];
            System.out.println(
                    history.changedDate + " / "
                            + history.beforeStatus + " → "
                            + history.afterStatus
            );
        }
    }

    // 운송장 번호가 같은 택배 객체를 찾는다.
    static Parcel findParcelByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcelCount; index++) {
            if (parcels[index].trackingNumber.equals(trackingNumber)) {
                return parcels[index];
            }
        }

        return null;
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        System.out.println("운송장 번호: " + parcel.trackingNumber);
        System.out.println("수령인: " + parcel.receiverName);
        System.out.println("연락처: " + parcel.receiverPhoneNumber);
        System.out.println("배송 지역: " + parcel.destination);
        System.out.println("배송 종류: " + parcel.deliveryType);
        System.out.println("무게: " + parcel.weight + "kg");
        System.out.println("배송비: " + parcel.fee + "원");
        System.out.println("상태: " + parcel.status);
        System.out.println("접수일: " + parcel.registeredDate);
        System.out.println("예상 도착: " + parcel.expectedDeliveryDays + "일 후");
    }

    // V1의 배송비 조건문을 그대로 옮긴 계산 메서드다.
    static int calculateFee(Parcel parcel) {
        int fee = 3000;

        if (parcel.weight >= 3) {
            fee += 2000;
        }

        if (parcel.destination.equals("제주")) {
            fee += 3000;
        }

        if (parcel.deliveryType == DeliveryType.특급) {
            fee += 2000;
        } else if (parcel.deliveryType == DeliveryType.냉장) {
            fee += 4000;
        } else if (parcel.deliveryType == DeliveryType.해외) {
            fee += 15000;
        }

        return fee;
    }

    // 배송 종류별 예상 도착 일수를 계산한다.
    static int calculateExpectedDeliveryDays(DeliveryType deliveryType) {
        if (deliveryType == DeliveryType.특급 || deliveryType == DeliveryType.냉장) {
            return 1;
        }

        if (deliveryType == DeliveryType.해외) {
            return 7;
        }

        return 3;
    }

    // 정해진 배송 종류 중 하나를 입력받는다.
    static DeliveryType readDeliveryType() {
        while (true) {
            String deliveryTypeInput = readLine("배송 종류(일반/특급/냉장/해외): ");

            if (deliveryTypeInput.equals("일반")) {
                return DeliveryType.일반;
            }

            if (deliveryTypeInput.equals("특급")) {
                return DeliveryType.특급;
            }

            if (deliveryTypeInput.equals("냉장")) {
                return DeliveryType.냉장;
            }

            if (deliveryTypeInput.equals("해외")) {
                return DeliveryType.해외;
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

// 택배 한 건과 그 배송 이력을 관리하는 클래스다.
class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    DeliveryType deliveryType;
    int weight;
    int fee;
    ParcelStatus status;
    String registeredDate;
    int expectedDeliveryDays;

    // ========== CH06 변경 ==========
    // 배송 이력도 택배 객체 안에서 관리한다.
    // =================================
    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;

    // 택배의 기본 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, DeliveryType deliveryType, int weight, String registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.deliveryType = deliveryType;
        this.weight = weight;
        this.registeredDate = registeredDate;
        this.status = ParcelStatus.접수;
    }

    // 이 택배의 상태 변경 이력을 추가한다.
    void addHistory(String beforeStatus, String afterStatus, String changedDate) {
        histories[historyCount] = new DeliveryHistory(beforeStatus, afterStatus, changedDate);
        historyCount++;
    }
}

// 택배 상태가 바뀐 시점을 기록하는 클래스다.
class DeliveryHistory {
    String beforeStatus;
    String afterStatus;
    String changedDate;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(String beforeStatus, String afterStatus, String changedDate) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedDate = changedDate;
    }
}

