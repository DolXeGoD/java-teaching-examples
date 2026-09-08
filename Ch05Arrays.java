import java.util.Scanner;

public class Ch05Arrays {
    // Ch05 V1: 아직 사용자 정의 메서드를 만들지 않고 main 안에서 모든 기능을 처리한다.
    public static void main(String[] args) {
        // 키보드로 입력한 값을 읽는 객체다.
        Scanner scanner = new Scanner(System.in);

        // 택배 정보를 같은 인덱스로 관리하는 병렬 배열이다.
        String[] trackingNumbers = new String[100]; // 운송장 번호
        String[] receiverNames = new String[100]; // 수령인 이름
        String[] receiverPhoneNumbers = new String[100]; // 수령인 연락처
        String[] destinations = new String[100]; // 배송 지역
        String[] deliveryTypes = new String[100]; // 일반, 특급, 냉장, 해외 등의 배송 종류
        int[] weights = new int[100]; // 택배 무게(kg)
        int[] fees = new int[100]; // 계산된 배송비
        String[] statuses = new String[100]; // 현재 배송 상태
        String[] registeredDates = new String[100]; // 택배를 접수한 날짜
        int[] expectedDeliveryDays = new int[100]; // 예상 배송 소요일
        int parcelCount = 0; // 현재 접수되어 배열에 저장된 택배 수

        // 배송 이력을 같은 인덱스로 관리하는 병렬 배열이다.
        String[] historyTrackingNumbers = new String[500]; // 이력이 남은 택배의 운송장 번호
        String[] beforeStatuses = new String[500]; // 상태가 바뀌기 전의 상태
        String[] afterStatuses = new String[500]; // 상태가 바뀐 후의 상태
        String[] changedDates = new String[500]; // 상태를 변경한 날짜
        int historyCount = 0; // 현재 저장된 배송 이력 수

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

            int menu = scanner.nextInt(); // 사용자가 선택한 메뉴 번호
            scanner.nextLine();

            switch (menu) {
                case 1:
                    if (parcelCount == trackingNumbers.length) {
                        System.out.println("더 이상 택배를 접수할 수 없습니다.");
                        break;
                    }

                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine(); // 새로 접수할 택배의 운송장 번호
                    boolean duplicated = false; // 같은 운송장 번호가 이미 있는지 확인하는 값

                    // index는 현재 중복 여부를 확인하는 택배의 배열 위치다.
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
                    String receiverName = scanner.nextLine(); // 새 택배 수령인의 이름
                    System.out.print("수령인 연락처: ");
                    String receiverPhoneNumber = scanner.nextLine(); // 새 택배 수령인의 연락처
                    System.out.print("배송 지역: ");
                    String destination = scanner.nextLine(); // 새 택배의 배송 지역
                    System.out.print("무게(kg): ");
                    int weight = scanner.nextInt(); // 새 택배의 무게
                    scanner.nextLine();
                    System.out.print("배송 종류(일반/특급/냉장/해외): ");
                    String deliveryType = scanner.nextLine(); // 새 택배의 배송 종류
                    System.out.print("접수일(예: 2026-09-01): ");
                    String registeredDate = scanner.nextLine(); // 새 택배를 접수한 날짜

                    int fee = 3000; // 기본 배송비에서 추가 비용을 계산할 변수
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

                    int expectedDays = 3; // 기본 예상 배송일에서 배송 종류에 따라 변경할 변수
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
                    trackingNumber = scanner.nextLine(); // 조회할 택배의 운송장 번호
                    int foundIndex = -1; // 찾은 택배의 배열 위치, 찾지 못하면 -1

                    // index는 현재 운송장 번호를 비교하는 택배의 배열 위치다.
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

                    // index는 현재 출력할 택배의 배열 위치다.
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
                    System.out.print("출고할 운송장 번호: ");
                    trackingNumber = scanner.nextLine(); // 출고할 택배의 운송장 번호
                    foundIndex = -1; // 찾은 택배의 배열 위치, 찾지 못하면 -1

                    // index는 현재 운송장 번호를 비교하는 택배의 배열 위치다.
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
                        System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
                        break;
                    }

                    System.out.print("출고일: ");
                    String shippedDate = scanner.nextLine(); // 출고 처리한 날짜
                    historyTrackingNumbers[historyCount] = trackingNumber;
                    beforeStatuses[historyCount] = statuses[foundIndex];
                    afterStatuses[historyCount] = "출고";
                    changedDates[historyCount] = shippedDate;
                    historyCount++;
                    statuses[foundIndex] = "출고";
                    System.out.println("출고 처리했습니다.");
                    break;

                case 5:
                    System.out.print("취소할 운송장 번호: ");
                    trackingNumber = scanner.nextLine(); // 취소할 택배의 운송장 번호
                    foundIndex = -1; // 찾은 택배의 배열 위치, 찾지 못하면 -1

                    // index는 현재 운송장 번호를 비교하는 택배의 배열 위치다.
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
                        System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
                        break;
                    }

                    System.out.print("취소일: ");
                    String canceledDate = scanner.nextLine(); // 취소 처리한 날짜
                    historyTrackingNumbers[historyCount] = trackingNumber;
                    beforeStatuses[historyCount] = statuses[foundIndex];
                    afterStatuses[historyCount] = "취소";
                    changedDates[historyCount] = canceledDate;
                    historyCount++;
                    statuses[foundIndex] = "취소";
                    System.out.println("배송을 취소했습니다.");
                    break;

                case 6:
                    System.out.print("운송장 번호: ");
                    trackingNumber = scanner.nextLine(); // 이력을 조회할 택배의 운송장 번호
                    boolean historyFound = false; // 해당 운송장 번호의 이력을 찾았는지 확인하는 값

                    // index는 현재 확인하는 배송 이력의 배열 위치다.
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
