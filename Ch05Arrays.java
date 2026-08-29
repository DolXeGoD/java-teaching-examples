import java.util.Scanner;

public class Ch05Arrays {
    // Ch05 V1: 아직 사용자 정의 메서드를 만들지 않고 main 안에서 모든 기능을 처리한다.
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 택배 정보를 같은 인덱스로 관리하는 병렬 배열이다.
        String[] trackingNumbers = new String[100];
        String[] receiverNames = new String[100];
        String[] receiverPhoneNumbers = new String[100];
        String[] destinations = new String[100];
        String[] deliveryTypes = new String[100];
        int[] weights = new int[100];
        int[] fees = new int[100];
        String[] statuses = new String[100];
        String[] registeredDates = new String[100];
        int[] expectedDeliveryDays = new int[100];
        int parcelCount = 0;

        // 배송 이력을 같은 인덱스로 관리하는 병렬 배열이다.
        String[] historyTrackingNumbers = new String[500];
        String[] beforeStatuses = new String[500];
        String[] afterStatuses = new String[500];
        String[] changedDates = new String[500];
        int historyCount = 0;

        while (true) {
            System.out.println("\n========== 당일배송 물류센터 ==========");
            System.out.println("1. 택배 접수");
            System.out.println("2. 운송장 번호로 조회");
            System.out.println("3. 전체 택배 조회");
            System.out.println("4. 출고 처리");
            System.out.println("5. 배송 취소");
            System.out.println("6. 배송 이력 조회");
            System.out.println("0. 종료");
            System.out.println("=====================================");
            System.out.print("메뉴 선택: ");

            int menu = scanner.nextInt();
            scanner.nextLine();

            switch (menu) {
                case 1:
                    if (parcelCount == trackingNumbers.length) {
                        System.out.println("더 이상 택배를 접수할 수 없습니다.");
                        break;
                    }

                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    boolean duplicated = false;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(trackingNumber)) {
                            duplicated = true;
                            break;
                        }
                    }

                    if (duplicated) {
                        System.out.println("이미 사용 중인 운송장 번호입니다.");
                        break;
                    }

                    System.out.print("수령인 이름: ");
                    String receiverName = scanner.nextLine();
                    System.out.print("수령인 연락처: ");
                    String receiverPhoneNumber = scanner.nextLine();
                    System.out.print("배송 지역: ");
                    String destination = scanner.nextLine();
                    System.out.print("무게(kg): ");
                    int weight = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("배송 종류(일반/특급/냉장/해외): ");
                    String deliveryType = scanner.nextLine();
                    System.out.print("접수일(예: 2026-09-01): ");
                    String registeredDate = scanner.nextLine();

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

                    int expectedDays = 3;
                    if (deliveryType.equals("특급") || deliveryType.equals("냉장")) {
                        expectedDays = 1;
                    } else if (deliveryType.equals("해외")) {
                        expectedDays = 7;
                    }

                    trackingNumbers[parcelCount] = trackingNumber;
                    receiverNames[parcelCount] = receiverName;
                    receiverPhoneNumbers[parcelCount] = receiverPhoneNumber;
                    destinations[parcelCount] = destination;
                    deliveryTypes[parcelCount] = deliveryType;
                    weights[parcelCount] = weight;
                    fees[parcelCount] = fee;
                    statuses[parcelCount] = "접수";
                    registeredDates[parcelCount] = registeredDate;
                    expectedDeliveryDays[parcelCount] = expectedDays;
                    parcelCount++;

                    historyTrackingNumbers[historyCount] = trackingNumber;
                    beforeStatuses[historyCount] = "없음";
                    afterStatuses[historyCount] = "접수";
                    changedDates[historyCount] = registeredDate;
                    historyCount++;

                    System.out.println("택배가 접수되었습니다. 배송비: " + fee + "원");
                    break;

                case 2:
                    System.out.print("운송장 번호: ");
                    trackingNumber = scanner.nextLine();
                    int foundIndex = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(trackingNumber)) {
                            foundIndex = index;
                            break;
                        }
                    }

                    if (foundIndex == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    System.out.println("운송장 번호: " + trackingNumbers[foundIndex]);
                    System.out.println("수령인: " + receiverNames[foundIndex]);
                    System.out.println("연락처: " + receiverPhoneNumbers[foundIndex]);
                    System.out.println("배송 지역: " + destinations[foundIndex]);
                    System.out.println("배송 종류: " + deliveryTypes[foundIndex]);
                    System.out.println("무게: " + weights[foundIndex] + "kg");
                    System.out.println("배송비: " + fees[foundIndex] + "원");
                    System.out.println("상태: " + statuses[foundIndex]);
                    System.out.println("접수일: " + registeredDates[foundIndex]);
                    System.out.println("예상 도착: " + expectedDeliveryDays[foundIndex] + "일 후");
                    break;

                case 3:
                    if (parcelCount == 0) {
                        System.out.println("접수된 택배가 없습니다.");
                        break;
                    }

                    for (int index = 0; index < parcelCount; index++) {
                        System.out.println(
                                trackingNumbers[index] + " / "
                                        + receiverNames[index] + " / "
                                        + deliveryTypes[index] + " / "
                                        + statuses[index]
                        );
                    }
                    break;

                case 4:
                case 5:
                    String action = menu == 4 ? "출고" : "취소";
                    System.out.print(action + "할 운송장 번호: ");
                    trackingNumber = scanner.nextLine();
                    foundIndex = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(trackingNumber)) {
                            foundIndex = index;
                            break;
                        }
                    }

                    if (foundIndex == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }
                    if (!statuses[foundIndex].equals("접수")) {
                        System.out.println("접수 상태의 택배만 처리할 수 있습니다.");
                        break;
                    }

                    System.out.print(action + "일: ");
                    String changedDate = scanner.nextLine();
                    historyTrackingNumbers[historyCount] = trackingNumber;
                    beforeStatuses[historyCount] = statuses[foundIndex];
                    afterStatuses[historyCount] = action;
                    changedDates[historyCount] = changedDate;
                    historyCount++;
                    statuses[foundIndex] = action;
                    System.out.println(action + " 처리했습니다.");
                    break;

                case 6:
                    System.out.print("운송장 번호: ");
                    trackingNumber = scanner.nextLine();
                    boolean historyFound = false;

                    for (int index = 0; index < historyCount; index++) {
                        if (historyTrackingNumbers[index].equals(trackingNumber)) {
                            System.out.println(
                                    changedDates[index] + " / "
                                            + beforeStatuses[index] + " → "
                                            + afterStatuses[index]
                            );
                            historyFound = true;
                        }
                    }

                    if (!historyFound) {
                        System.out.println("배송 이력이 없습니다.");
                    }
                    break;

                case 0:
                    System.out.println("프로그램을 종료합니다.");
                    return;

                default:
                    System.out.println("메뉴 번호를 다시 입력하세요.");
            }
        }
    }
}
