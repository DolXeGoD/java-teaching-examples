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

public class Ch06Classes {
    // ========== CH06 클래스 변경 ==========
    // 병렬 배열 대신 택배 객체 배열을 사용한다.
    // 아직 메서드를 배우기 전이므로 모든 처리 과정은 main 안에 작성한다.
    // =================================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Parcel[] parcels = new Parcel[100];
        int parcelCount = 0;

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
                case 1: {
                    if (parcelCount == parcels.length) {
                        System.out.println("더 이상 택배를 접수할 수 없습니다.");
                        break;
                    }

                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    boolean isExistNumber = false;

                    for (int index = 0; index < parcelCount; index++) {
                        if (parcels[index].trackingNumber.equals(trackingNumber)) {
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

                    int expectedDeliveryDates = 3;
                    if (deliveryType == DeliveryType.특급) {
                        expectedDeliveryDates = 1;
                    } else if (deliveryType == DeliveryType.냉장) {
                        expectedDeliveryDates = 1;
                    } else if (deliveryType == DeliveryType.해외) {
                        expectedDeliveryDates = 7;
                    }

                    // 생성자를 배우기 전이므로 기본 생성자로 객체를 만든 뒤 필드에 값을 넣는다.
                    Parcel parcel = new Parcel();
                    parcel.trackingNumber = trackingNumber;
                    parcel.receiverName = receiverName;
                    parcel.receiverPhoneNumber = receiverPhoneNumber;
                    parcel.destination = destination;
                    parcel.deliveryType = deliveryType;
                    parcel.weight = weight;
                    parcel.deliveryFee = deliveryFee;
                    parcel.parcelStatus = ParcelStatus.접수;
                    parcel.registeredDate = registeredDate;
                    parcel.expectedDeliveryDates = expectedDeliveryDates;

                    DeliveryHistory history = new DeliveryHistory();
                    history.beforeParcelStatus = ParcelStatus.없음;
                    history.afterParcelStatus = ParcelStatus.접수;
                    history.historyChangedDate = registeredDate;
                    parcel.histories[parcel.historyCount] = history;
                    parcel.historyCount++;

                    parcels[parcelCount] = parcel;
                    parcelCount++;

                    System.out.println("택배가 접수되었습니다. 배송비: " + parcel.deliveryFee + "원");
                    break;
                }

                case 2: {
                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (parcels[index].trackingNumber.equals(trackingNumber)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    Parcel parcel = parcels[targetPosition];
                    System.out.println("운송장 번호: " + parcel.trackingNumber);
                    System.out.println("수령인: " + parcel.receiverName);
                    System.out.println("연락처: " + parcel.receiverPhoneNumber);
                    System.out.println("배송 지역: " + parcel.destination);
                    System.out.println("배송 종류: " + parcel.deliveryType);
                    System.out.println("무게: " + parcel.weight + "kg");
                    System.out.println("배송비: " + parcel.deliveryFee + "원");
                    System.out.println("상태: " + parcel.parcelStatus);
                    System.out.println("접수일: " + parcel.registeredDate);
                    System.out.println("예상 도착: " + parcel.expectedDeliveryDates + "일 후");
                    break;
                }

                case 3: {
                    if (parcelCount == 0) {
                        System.out.println("접수된 택배가 없습니다.");
                        break;
                    }

                    for (int index = 0; index < parcelCount; index++) {
                        Parcel parcel = parcels[index];
                        System.out.println(
                                parcel.trackingNumber + " / "
                                        + parcel.receiverName + " / "
                                        + parcel.deliveryType + " / "
                                        + parcel.parcelStatus
                        );
                    }
                    break;
                }

                case 4: {
                    System.out.print("출고할 운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (parcels[index].trackingNumber.equals(trackingNumber)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    Parcel parcel = parcels[targetPosition];
                    if (parcel.parcelStatus != ParcelStatus.접수) {
                        System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
                        break;
                    }

                    System.out.print("출고일: ");
                    String shippedDate = scanner.nextLine();

                    DeliveryHistory history = new DeliveryHistory();
                    history.beforeParcelStatus = ParcelStatus.접수;
                    history.afterParcelStatus = ParcelStatus.출고;
                    history.historyChangedDate = shippedDate;
                    parcel.histories[parcel.historyCount] = history;
                    parcel.historyCount++;
                    parcel.parcelStatus = ParcelStatus.출고;

                    System.out.println("출고 처리했습니다.");
                    break;
                }

                case 5: {
                    System.out.print("취소할 운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (parcels[index].trackingNumber.equals(trackingNumber)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    Parcel parcel = parcels[targetPosition];
                    if (parcel.parcelStatus != ParcelStatus.접수) {
                        System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
                        break;
                    }

                    System.out.print("취소일: ");
                    String canceledDate = scanner.nextLine();

                    DeliveryHistory history = new DeliveryHistory();
                    history.beforeParcelStatus = ParcelStatus.접수;
                    history.afterParcelStatus = ParcelStatus.취소;
                    history.historyChangedDate = canceledDate;
                    parcel.histories[parcel.historyCount] = history;
                    parcel.historyCount++;
                    parcel.parcelStatus = ParcelStatus.취소;

                    System.out.println("배송을 취소했습니다.");
                    break;
                }

                case 6: {
                    System.out.print("운송장 번호: ");
                    String trackingNumber = scanner.nextLine();
                    int targetPosition = -1;

                    for (int index = 0; index < parcelCount; index++) {
                        if (parcels[index].trackingNumber.equals(trackingNumber)) {
                            targetPosition = index;
                            break;
                        }
                    }

                    if (targetPosition == -1) {
                        System.out.println("존재하지 않는 운송장 번호입니다.");
                        break;
                    }

                    Parcel parcel = parcels[targetPosition];
                    for (int index = 0; index < parcel.historyCount; index++) {
                        DeliveryHistory history = parcel.histories[index];
                        System.out.println(
                                history.historyChangedDate + " / "
                                        + history.beforeParcelStatus + " → "
                                        + history.afterParcelStatus
                        );
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

// 택배 한 건의 데이터를 묶어서 보관하는 클래스다.
class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    DeliveryType deliveryType;
    int weight;
    int deliveryFee;
    ParcelStatus parcelStatus;
    String registeredDate;
    int expectedDeliveryDates;
    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;
}

// 택배 상태가 바뀐 기록 한 건을 보관하는 클래스다.
class DeliveryHistory {
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    String historyChangedDate;
}
