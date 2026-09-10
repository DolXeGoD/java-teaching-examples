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
    취소,
    없음
}

public class Ch05Arrays {
    // Ch05 V1: 아직 사용자 정의 메서드를 만들지 않고 main 안에서 모든 기능을 처리한다.
    public static void main(String[] args) {
        // 1. 운송장 번호
        String[] trackingNumbers = new String[100];
        // 2. 수령인 이름
        String[] receiverNames = new String[100];
        // 3. 수령인 연락처
        String[] receiverPhoneNumbers = new String[100];
        // 4. 배송 지역
        String[] destinations = new String[100];
        // 5. 배송 종류
        DeliveryType[] deliveryTypes = new DeliveryType[100];
        // 6. 택배 무게
        int[] weights = new int[100];
        // 7. 계산된 배송비
        int[] fees = new int[100];
        // 8. 현재 배송 상태
        ParcelStatus[] parcelStatus = new ParcelStatus[100];
        // 9. 택배 접수일
        String[] registeredDates = new String[100];
        // 10. 예상 배송 소요일
        int[] expectedDeliveryDates = new int[100];
        int parcelCount = 0;

        // 11. 이력이 남은 택배의 운송장 번호
        String[] historyTrackingNumbers = new String[500];
        // 12. 변경 전 상태
        ParcelStatus[] beforeParcelStatus = new ParcelStatus[500];
        // 13. 변경 후 상태
        ParcelStatus[] afterParcelStatus = new ParcelStatus[500];
        // 14. 변경 날짜
        String[] historyChangedDates = new String[500];
        int historyCount = 0;

        Scanner scanner = new Scanner(System.in);

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

            int menu = Integer.parseInt(scanner.nextLine());

            switch (menu) {
                case 1:
                    if (parcelCount == trackingNumbers.length) {
                        System.out.println("더 이상 택배를 접수할 수 없습니다.");
                        break;
                    }

                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    boolean isExistNumber = false;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(trackingNumber)) {
                            isExistNumber = true;
                            break;
                        }
                    }

                    if (isExistNumber) {
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
                    int weight = Integer.parseInt(scanner.nextLine());
                    System.out.print("배송 종류(일반/특급/냉장/해외): ");
                    String deliveryTypeString = scanner.nextLine();
                    DeliveryType deliveryType = null;

                    if (deliveryTypeString.equals("일반")) {
                        deliveryType = DeliveryType.일반;
                    } else if (deliveryTypeString.equals("특급")) {
                        deliveryType = DeliveryType.특급;
                    } else if (deliveryTypeString.equals("냉장")) {
                        deliveryType = DeliveryType.냉장;
                    } else if (deliveryTypeString.equals("해외")) {
                        deliveryType = DeliveryType.해외;
                    }

                    if (deliveryType == null) {
                        System.out.println("배송 종류를 다시 입력하세요.");
                        break;
                    }

                    System.out.print("접수일(예: 2026-09-01): ");
                    String registeredDate = scanner.nextLine();

                    int deliveryFee = 3000;
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

                    int expectedDays = 3;
                    if (deliveryType == DeliveryType.특급) {
                        expectedDays = 1;
                    } else if (deliveryType == DeliveryType.냉장) {
                        expectedDays = 1;
                    } else if (deliveryType == DeliveryType.해외) {
                        expectedDays = 7;
                    }

                    trackingNumbers[parcelCount] = trackingNumber;
                    receiverNames[parcelCount] = receiverName;
                    receiverPhoneNumbers[parcelCount] = receiverPhoneNumber;
                    destinations[parcelCount] = destination;
                    deliveryTypes[parcelCount] = deliveryType;
                    weights[parcelCount] = weight;
                    fees[parcelCount] = deliveryFee;
                    parcelStatus[parcelCount] = ParcelStatus.접수;
                    registeredDates[parcelCount] = registeredDate;
                    expectedDeliveryDates[parcelCount] = expectedDays;
                    parcelCount++;

                    historyTrackingNumbers[historyCount] = trackingNumber;
                    beforeParcelStatus[historyCount] = ParcelStatus.없음;
                    afterParcelStatus[historyCount] = ParcelStatus.접수;
                    historyChangedDates[historyCount] = registeredDate;
                    historyCount++;

                    System.out.println("택배가 접수되었습니다. 배송비: " + deliveryFee + "원");
                    break;

                case 2: {
                    System.out.print("운송장 번호: ");
                    String searchTarget = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(searchTarget)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    System.out.println("운송장 번호: " + trackingNumbers[targetPosition]);
                    System.out.println("수령인: " + receiverNames[targetPosition]);
                    System.out.println("연락처: " + receiverPhoneNumbers[targetPosition]);
                    System.out.println("배송 지역: " + destinations[targetPosition]);
                    System.out.println("배송 종류: " + deliveryTypes[targetPosition]);
                    System.out.println("무게: " + weights[targetPosition] + "kg");
                    System.out.println("배송비: " + fees[targetPosition] + "원");
                    System.out.println("상태: " + parcelStatus[targetPosition]);
                    System.out.println("접수일: " + registeredDates[targetPosition]);
                    System.out.println("예상 도착: " + expectedDeliveryDates[targetPosition] + "일 후");
                    break;
                }

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
                                        + parcelStatus[index]
                        );
                    }
                    break;

                case 4: {
                    System.out.print("출고할 운송장 번호: ");
                    String searchTarget = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(searchTarget)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }
                    if (parcelStatus[targetPosition] != ParcelStatus.접수) {
                        System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
                        break;
                    }

                    System.out.print("출고일: ");
                    String shippedDate = scanner.nextLine();
                    historyTrackingNumbers[historyCount] = searchTarget;
                    beforeParcelStatus[historyCount] = ParcelStatus.접수;
                    afterParcelStatus[historyCount] = ParcelStatus.출고;
                    historyChangedDates[historyCount] = shippedDate;
                    historyCount++;
                    parcelStatus[targetPosition] = ParcelStatus.출고;
                    System.out.println("출고 처리했습니다.");
                    break;
                }

                case 5: {
                    System.out.print("취소할 운송장 번호: ");
                    String searchTarget = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (trackingNumbers[index].equals(searchTarget)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }
                    if (parcelStatus[targetPosition] != ParcelStatus.접수) {
                        System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
                        break;
                    }

                    System.out.print("취소일: ");
                    String canceledDate = scanner.nextLine();
                    historyTrackingNumbers[historyCount] = searchTarget;
                    beforeParcelStatus[historyCount] = ParcelStatus.접수;
                    afterParcelStatus[historyCount] = ParcelStatus.취소;
                    historyChangedDates[historyCount] = canceledDate;
                    historyCount++;
                    parcelStatus[targetPosition] = ParcelStatus.취소;
                    System.out.println("배송을 취소했습니다.");
                    break;
                }

                case 6: {
                    System.out.print("운송장 번호: ");
                    String searchTarget = scanner.nextLine();
                    boolean isExistHistory = false;

                    for (int index = 0; index < historyCount; index++) {
                        if (historyTrackingNumbers[index].equals(searchTarget)) {
                            System.out.println(
                                    historyChangedDates[index] + " / "
                                            + beforeParcelStatus[index] + " → "
                                            + afterParcelStatus[index]
                            );
                            isExistHistory = true;
                        }
                    }

                    if (!isExistHistory) {
                        System.out.println("배송 이력이 없습니다.");
                    }
                    break;
                }

                case 0:
                    System.out.println("프로그램을 종료합니다.");
                    return;

                default:
                    System.out.println("메뉴 번호를 다시 입력하세요.");
            }
        }
    }
}
