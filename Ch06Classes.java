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

public class Ch06Classes {
    public static void main(String[] args) {

        Parcel[] parcels = new Parcel[100];
        DeliveryHistory[] histories = new DeliveryHistory[100];

        int parcelCount = 0;
        int historyCount = 0;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("======= 택배 관리 시스템 =======");
            System.out.println("1. 택배 접수");
            System.out.println("2. 운송장 번호로 조회");
            System.out.println("3. 전체 택배 조회");
            System.out.println("4. 출고 처리");
            System.out.println("5. 배송 취소");
            System.out.println("6. 배송 이력 조회");
            System.out.println("0. 프로그램 종료");
            System.out.println("============================");
            System.out.print("메뉴 선택 : ");

            int menu = Integer.parseInt(scanner.nextLine());

            switch (menu) {
                case 0:
                    if(parcelCount > 0) {
                        System.out.println("현재 택배가 1개 이상 관리되고 있습니다. 정말 종료하시겠습니까?");
                        System.out.print("종료하려면 Y를, 취소하려면 N을 입력해주세요 : ");
                        String decide = scanner.nextLine();

                        if (!decide.equals("Y")) {
                            System.out.println("종료를 취소하고 메뉴로 돌아갑니다.");
                            break;
                        }
                    }

                    System.out.println("프로그램을 종료합니다.");
                    return;

                case 1:
                    if (parcelCount == parcels.length) {
                        System.out.println("더 이상 택배를 접수할 수 없습니다.");
                        break;
                    }

                    // 각각의 정보를 받아서 배열에 넣어주기
                    // 1. 운송장 번호
                    System.out.print("운송장 번호를 입력하세요 : ");
                    String trackingNumber = scanner.nextLine();

                    // 1-1. 운송장 번호 중복 조회
                    boolean isExistNumber = false;
                    for(int i=0; i<parcelCount; i++){
                        if(parcels[i].trackingNumber.equals(trackingNumber)){
                            isExistNumber = true;
                            break;
                        }
                    }

                    if(isExistNumber){
                        System.out.println("이미 있는 운송장 번호입니다");
                        break;
                    }

                    // 2. 수령인 이름
                    System.out.print("수령인 이름 입력하세요 : ");
                    String receiverName = scanner.nextLine();
                    // 3. 수령인 연락처
                    System.out.print("수령인 연락처를 입력하세요 : ");
                    String receiverPhoneNumber = scanner.nextLine();
                    // 4. 배송 지역
                    System.out.print("배송 지역을 입력하세요 : ");
                    String destination = scanner.nextLine();
                    // 5. 무게
                    System.out.print("택배 무게를 입력하세요 : ");
                    int weight = Integer.parseInt(scanner.nextLine());
                    // 6. 배송 종류
                    System.out.print("배송 종류를 입력하세요 : ");
                    String deliveryTypeString = scanner.nextLine();
                    DeliveryType deliveryType = null;

                    // 6-1. 배송 종류 변환
                    if(deliveryTypeString.equals("일반")){
                        deliveryType = DeliveryType.일반;
                    } else if(deliveryTypeString.equals("특급")){
                        deliveryType = DeliveryType.특급;
                    } else if(deliveryTypeString.equals("냉장")){
                        deliveryType = DeliveryType.냉장;
                    } else if(deliveryTypeString.equals("해외")){
                        deliveryType = DeliveryType.해외;
                    }
                    // 오타 처리
                    if(deliveryType == null){
                        System.out.println("배송 종류가 올바르지 않습니다.");
                        break;
                    }

                    // 7. 택배 접수일
                    System.out.print("택배 접수일을 입력하세요 (예: 2026-09-09) : ");
                    String registeredDate = scanner.nextLine();

                    // 8. 배송비
                    int deliveryFee = 3000;
                    if (weight >= 3){ // 무게가 3kg 이상인 경우
                        deliveryFee += 2000;
                    }

                    if (destination.equals("제주")){
                        deliveryFee += 3000;
                    }

                    if (deliveryType == DeliveryType.특급) {
                        deliveryFee += 2000;
                    } else if (deliveryType == DeliveryType.냉장) {
                        deliveryFee += 4000;
                    } else if (deliveryType == DeliveryType.해외) {
                        deliveryFee += 15000;
                    }

                    // 배송일 계산
                    int expectedDays = 3; // 기본 예상 배송일에서 배송 종류에 따라 변경할 변수
                    if (deliveryType == DeliveryType.특급 || deliveryType == DeliveryType.냉장) {
                        expectedDays = 1;
                    } else if (deliveryType == DeliveryType.해외) {
                        expectedDays = 7;
                    }

                    // 정보 최종 삽입
                    Parcel parcel = new Parcel(
                            trackingNumber,
                            receiverName,
                            receiverPhoneNumber,
                            destination,
                            deliveryType,
                            weight,
                            deliveryFee,
                            ParcelStatus.접수,
                            registeredDate,
                            expectedDays
                    );

                    parcels[parcelCount] = parcel;
                    parcelCount++;

                    // 히스토리
                    DeliveryHistory deliveryHistory = new DeliveryHistory(
                            trackingNumber,
                            ParcelStatus.없음,
                            ParcelStatus.접수,
                            registeredDate
                    );

                    histories[historyCount] = deliveryHistory;
                    historyCount++;

                    System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);

                    break;
                case 2:
                    // 1. 사용자로부터 운송장 번호 입력받기
                    System.out.print("운송장 번호를 입력하세요 : ");
                    String searchTarget = scanner.nextLine();
                    int targetPosition = -1;

                    // 2. 운송장 번호 배열에서, 해당 운송장 번호 위치 찾기
                    for(int i=0; i<parcelCount; i++){
                        if(parcels[i].trackingNumber.equals(searchTarget)){
                            targetPosition = i;
                            break;
                        }
                    }

                    // 3. 못 찾았으면 못 찾겠다 하기
                    if(targetPosition == -1){
                        System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
                        break;
                    }

                    // 4. 위치 찾았으면, 그 위치 기반으로 다른 배열 싹 다 조회해서 택배 정보 보여주기
                    System.out.println("운송장 번호: " + parcels[targetPosition].trackingNumber);
                    System.out.println("수령인: " + parcels[targetPosition].receiverName);
                    System.out.println("연락처: " + parcels[targetPosition].receiverPhoneNumber);
                    System.out.println("배송 지역: " + parcels[targetPosition].destination);
                    System.out.println("배송 종류: " + parcels[targetPosition].deliveryType);
                    System.out.println("무게: " + parcels[targetPosition].weight + "kg");
                    System.out.println("배송비: " + parcels[targetPosition].deliveryFee + "원");
                    System.out.println("상태: " + parcels[targetPosition].parcelStatus);
                    System.out.println("접수일: " + parcels[targetPosition].registeredDate);
                    System.out.println("예상 도착: " + parcels[targetPosition].expectedDeliveryDates + "일 후");
                    break;
                case 3:
                    if (parcelCount == 0) {
                        System.out.println("접수된 택배가 없습니다.");
                        break;
                    }

                    for (int index = 0; index < parcelCount; index++) {
                        System.out.println(
                                parcels[index].trackingNumber + " / "
                                        + parcels[index].receiverName + " / "
                                        + parcels[index].deliveryType + " / "
                                        + parcels[index].parcelStatus
                        );
                    }
                    break;
                case 4: // 출고 처리
                    // 1. 출고할 운송장 번호 입력받기
                    System.out.print("출고할 운송장 번호 : ");
                    String searchTarget2 = scanner.nextLine();

                    int targetPosition2 = -1;

                    // 2. 운송장 번호 배열에서, 해당 운송장 번호 위치 찾기
                    for(int i=0; i<parcelCount; i++){
                        if(parcels[i].trackingNumber.equals(searchTarget2)){
                            targetPosition2 = i;
                            break;
                        }
                    }

                    if(targetPosition2 == -1){
                        System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
                        break;
                    }

                    if (parcels[targetPosition2].parcelStatus != ParcelStatus.접수) {
                        System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
                        break;
                    }

                    // 2. 접수 -> 출고 상태 변경
                    System.out.print("출고 날짜를 입력하세요 (예 2026-09-09) :");
                    String shippedDate = scanner.nextLine();

                    parcels[targetPosition2].parcelStatus = ParcelStatus.출고;

                    // 3. 히스토리 추가 (접수 -> 출고)

                    DeliveryHistory deliveryHistory2 = new DeliveryHistory(
                            searchTarget2,
                            ParcelStatus.접수,
                            ParcelStatus.출고,
                            shippedDate
                    );

                    histories[historyCount] = deliveryHistory2;
                    historyCount++;

                    // [예외 상황]
                    // - 운송장 번호 존재하지 않을 수 있음
                    // - "접수" 상태가 아닐 수 있음.
                    break;
                case 5: // 취소 처리 (접수 상태만)
                    // 1. 출고할 운송장 번호 입력받기
                    System.out.print("취소할 운송장 번호 : ");
                    String searchTarget3 = scanner.nextLine();

                    int targetPosition3 = -1;

                    // 2. 운송장 번호 배열에서, 해당 운송장 번호 위치 찾기
                    for(int i=0; i<parcelCount; i++){
                        if(parcels[i].trackingNumber.equals(searchTarget3)){
                            targetPosition3 = i;
                            break;
                        }
                    }

                    if(targetPosition3 == -1){
                        System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
                        break;
                    }

                    if (parcels[targetPosition3].parcelStatus != ParcelStatus.출고) {
                        System.out.println("출고 상태의 택배만 취소할 수 있습니다.");
                        break;
                    }

                    // 2. 출고 -> 취소 상태 변경
                    System.out.print("취소 날짜를 입력하세요 (예 2026-09-09) :");
                    String shippedDate2 = scanner.nextLine();

                    parcels[targetPosition3].parcelStatus = ParcelStatus.취소;

                    // 3. 히스토리 추가 (출고 -> 취소)
                    DeliveryHistory history = new DeliveryHistory(
                            searchTarget3,
                            ParcelStatus.출고,
                            ParcelStatus.취소,
                            shippedDate2
                    );

                    histories[historyCount] = history;
                    historyCount++;

                    break;
                case 6: // 이력 조회
                    // 운송장 번호 입력받아서
                    System.out.print("조회할 운송장 번호 : ");
                    String searchTarget4 = scanner.nextLine();

                    // history 에 해당 운송장번호로 기록된 내역을 모두 출력한다.
                    for(int i=0; i<historyCount; i++){
                        if(histories[i].historyTrackingNumber.equals(searchTarget4)){
                            System.out.println(
                                    histories[i].historyTrackingNumber + "|"
                                    + histories[i].historyChangedDate + "|"
                                    + histories[i].beforeParcelStatus + "|"
                                    + histories[i].afterParcelStatus
                            );
                        }
                    }

                    break;
                default: // 메뉴 잘못 선택한 경우
                    System.out.println("없는 메뉴입니다. 다시 선택해주세요.");
                    break;

            }
        }
    }
}

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

    public Parcel(
        String trackingNumber,
        String receiverName,
        String receiverPhoneNumber,
        String destination,
        DeliveryType deliveryType,
        int weight,
        int deliveryFee,
        ParcelStatus parcelStatus,
        String registeredDate,
        int expectedDeliveryDates
    ) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.deliveryType = deliveryType;
        this.weight = weight;
        this.deliveryFee = deliveryFee;
        this.parcelStatus = parcelStatus;
        this.registeredDate = registeredDate;
        this.expectedDeliveryDates = expectedDeliveryDates;
    }
}

class DeliveryHistory {
    String historyTrackingNumber;
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    String historyChangedDate;

    public DeliveryHistory(
        String historyTrackingNumber,
        ParcelStatus beforeParcelStatus,
        ParcelStatus afterParcelStatus,
        String historyChangedDate
    ) {
        this.historyTrackingNumber = historyTrackingNumber;
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDate = historyChangedDate;
    }
}
