import java.util.Scanner;

// ========== CH06 변경 ==========
// 길어진 main 메서드의 기능별 코드를 static 메서드로 분리한다.
// =================================
public class Ch06Methods {
    // 메뉴 입력에 사용하는 스캐너다.
    static Scanner scanner = new Scanner(System.in);

    // 택배 정보를 같은 인덱스로 관리하는 병렬 배열이다.
    static String[] trackingNumbers = new String[100];
    static String[] receiverNames = new String[100];
    static String[] receiverPhoneNumbers = new String[100];
    static String[] destinations = new String[100];
    static String[] deliveryTypes = new String[100];
    static int[] weights = new int[100];
    static int[] fees = new int[100];
    static String[] statuses = new String[100];
    static String[] registeredDates = new String[100];
    static int[] expectedDeliveryDays = new int[100];
    static int parcelCount = 0;

    // 배송 이력을 같은 인덱스로 관리하는 병렬 배열이다.
    static String[] historyTrackingNumbers = new String[500];
    static String[] beforeStatuses = new String[500];
    static String[] afterStatuses = new String[500];
    static String[] changedDates = new String[500];
    static int historyCount = 0;

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

    // 택배 정보를 배열에 추가한다.
    static void registerParcel() {
        if (parcelCount == trackingNumbers.length) {
            System.out.println("더 이상 택배를 접수할 수 없습니다.");
            return;
        }

        String trackingNumber = readLine("운송장 번호: ");
        if (findParcelIndex(trackingNumber) != -1) {
            System.out.println("이미 사용 중인 운송장 번호입니다.");
            return;
        }

        String receiverName = readLine("수령인 이름: ");
        String receiverPhoneNumber = readLine("수령인 연락처: ");
        String destination = readLine("배송 지역: ");
        int weight = readInt("무게(kg): ");
        String deliveryType = readDeliveryType();
        String registeredDate = readLine("접수일(예: 2026-09-01): ");

        trackingNumbers[parcelCount] = trackingNumber;
        receiverNames[parcelCount] = receiverName;
        receiverPhoneNumbers[parcelCount] = receiverPhoneNumber;
        destinations[parcelCount] = destination;
        deliveryTypes[parcelCount] = deliveryType;
        weights[parcelCount] = weight;
        fees[parcelCount] = calculateFee(destination, weight, deliveryType);
        statuses[parcelCount] = "접수";
        registeredDates[parcelCount] = registeredDate;
        expectedDeliveryDays[parcelCount] = calculateExpectedDeliveryDays(deliveryType);

        saveHistory(trackingNumber, "없음", "접수", registeredDate);
        parcelCount++;

        System.out.println("택배가 접수되었습니다. 배송비: " + fees[parcelCount - 1] + "원");
    }

    // 운송장 번호로 택배 한 건을 조회한다.
    static void findParcel() {
        String trackingNumber = readLine("운송장 번호: ");
        int index = findParcelIndex(trackingNumber);

        if (index == -1) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        printParcel(index);
    }

    // 현재 접수된 모든 택배를 출력한다.
    static void printAllParcels() {
        if (parcelCount == 0) {
            System.out.println("접수된 택배가 없습니다.");
            return;
        }

        for (int index = 0; index < parcelCount; index++) {
            System.out.println(
                    trackingNumbers[index] + " / "
                            + receiverNames[index] + " / "
                            + deliveryTypes[index] + " / "
                            + statuses[index]
            );
        }
    }

    // 접수 상태의 택배를 출고 상태로 바꾼다.
    static void shipParcel() {
        String trackingNumber = readLine("출고할 운송장 번호: ");
        int index = findParcelIndex(trackingNumber);

        if (index == -1) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        if (!statuses[index].equals("접수")) {
            System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
            return;
        }

        statuses[index] = "출고";
        saveHistory(trackingNumber, "접수", "출고", readLine("출고일: "));
        System.out.println("출고 처리했습니다.");
    }

    // 접수 상태의 택배를 취소 상태로 바꾼다.
    static void cancelParcel() {
        String trackingNumber = readLine("취소할 운송장 번호: ");
        int index = findParcelIndex(trackingNumber);

        if (index == -1) {
            System.out.println("존재하지 않는 운송장 번호입니다.");
            return;
        }

        if (!statuses[index].equals("접수")) {
            System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
            return;
        }

        statuses[index] = "취소";
        saveHistory(trackingNumber, "접수", "취소", readLine("취소일: "));
        System.out.println("배송을 취소했습니다.");
    }

    // 운송장 번호에 해당하는 배송 이력을 출력한다.
    static void printHistory() {
        String trackingNumber = readLine("운송장 번호: ");
        boolean found = false;

        for (int index = 0; index < historyCount; index++) {
            if (historyTrackingNumbers[index].equals(trackingNumber)) {
                System.out.println(
                        changedDates[index] + " / "
                                + beforeStatuses[index] + " → "
                                + afterStatuses[index]
                );
                found = true;
            }
        }

        if (!found) {
            System.out.println("배송 이력이 없습니다.");
        }
    }

    // 배송 이력을 병렬 배열에 저장한다.
    static void saveHistory(String trackingNumber, String beforeStatus,
                            String afterStatus, String changedDate) {
        historyTrackingNumbers[historyCount] = trackingNumber;
        beforeStatuses[historyCount] = beforeStatus;
        afterStatuses[historyCount] = afterStatus;
        changedDates[historyCount] = changedDate;
        historyCount++;
    }

    // 운송장 번호가 저장된 배열 위치를 찾는다.
    static int findParcelIndex(String trackingNumber) {
        for (int index = 0; index < parcelCount; index++) {
            if (trackingNumbers[index].equals(trackingNumber)) {
                return index;
            }
        }

        return -1;
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(int index) {
        System.out.println("운송장 번호: " + trackingNumbers[index]);
        System.out.println("수령인: " + receiverNames[index]);
        System.out.println("연락처: " + receiverPhoneNumbers[index]);
        System.out.println("배송 지역: " + destinations[index]);
        System.out.println("배송 종류: " + deliveryTypes[index]);
        System.out.println("무게: " + weights[index] + "kg");
        System.out.println("배송비: " + fees[index] + "원");
        System.out.println("상태: " + statuses[index]);
        System.out.println("접수일: " + registeredDates[index]);
        System.out.println("예상 도착: " + expectedDeliveryDays[index] + "일 후");
    }

    // 배송 지역, 무게, 배송 종류로 배송비를 계산한다.
    static int calculateFee(String destination, int weight, String deliveryType) {
        int fee = 3000;

        if (weight >= 3) {
            fee += 2000;
        }

        if (destination.equals("제주")) {
            fee += 3000;
        }

        if (deliveryType.equals("특급")) {
            fee += 2000;
        } else if (deliveryType.equals("냉장")) {
            fee += 4000;
        } else if (deliveryType.equals("해외")) {
            fee += 15000;
        }

        return fee;
    }

    // 배송 종류별 예상 도착까지 걸리는 일수를 계산한다.
    static int calculateExpectedDeliveryDays(String deliveryType) {
        if (deliveryType.equals("특급")) {
            return 1;
        }

        if (deliveryType.equals("냉장")) {
            return 1;
        }

        if (deliveryType.equals("해외")) {
            return 7;
        }

        return 3;
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
