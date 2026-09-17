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




public class Ch06Methods {
    static Scanner scanner = new Scanner(System.in);
    static Parcel[] parcels = new Parcel[100];
    static int parcelCount = 0;

    public static void main(String[] args) {
        while (true) {
            printMenu();
            int menu = readInt("메뉴 선택 : ");

            switch (menu) {
                case 0:
                    if(parcelCount > 0) {
                        System.out.println("현재 택배가 1개 이상 관리되고 있습니다. 정말 종료하시겠습니까?");
                        String decide = readLine("종료하려면 Y를, 취소하려면 N을 입력해주세요 : ");

                        if (!decide.equals("Y")) {
                            System.out.println("종료를 취소하고 메뉴로 돌아갑니다.");
                            break;
                        }
                    }

                    System.out.println("프로그램을 종료합니다.");
                    return;
                case 1:
                    registerParcel();
                    break;
                case 2:
                    findParcel();
                    break;
                case 3:
                    // 전체 택배 조회
                    printAllParcels();
                    break;
                case 4: // 출고 처리
                    shipParcel();
                    break;
                case 5: // 취소 처리 (접수 상태만)
                     cancelParcel();
                    break;
                case 6: // 이력 조회
                     printHistory();
                    break;
                default: // 메뉴 잘못 선택한 경우
                    System.out.println("없는 메뉴입니다. 다시 선택해주세요.");
                    break;

            }
        }
    }

    // 여러 메뉴에서 반복되는 숫자 입력 처리를 readInt로 분리한다.
    // 입력 안내 후 정수를 입력받아 반환한다.
    static int readInt(String message) {
        System.out.print(message);
        return Integer.parseInt(scanner.nextLine());
    }

    // 여러 메뉴에서 반복되는 문자열 입력 처리를 readLine으로 분리한다.
    // 입력 안내 후 문자열을 입력받아 반환한다.
    static String readLine(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    // 메뉴 출력 메서드
    static void printMenu() {
        System.out.println("======= 택배 관리 시스템 =======");
        System.out.println("1. 택배 접수");
        System.out.println("2. 운송장 번호로 조회");
        System.out.println("3. 전체 택배 조회");
        System.out.println("4. 출고 처리");
        System.out.println("5. 배송 취소");
        System.out.println("6. 배송 이력 조회");
        System.out.println("0. 프로그램 종료");
        System.out.println("============================");
    }
    // 택배 접수
    static void registerParcel() {
        if (parcelCount == parcels.length) {
            System.out.println("더 이상 택배를 접수할 수 없습니다.");
            return;
        }

        // 각각의 정보를 받아서 배열에 넣어주기
        // 1. 운송장 번호
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");

        // 1-1. 운송장 번호 중복 조회
        if (findParcelByTrackingNumber(trackingNumber) != null) {
            System.out.println("이미 있는 운송장 번호입니다");
            return;
        }

        // 2. 수령인 이름
        String receiverName = readLine("수령인 이름 입력하세요 : ");
        // 3. 수령인 연락처
        String receiverPhoneNumber = readLine("수령인 연락처를 입력하세요 : ");
        // 4. 배송 지역
        String destination = readLine("배송 지역을 입력하세요 : ");
        // 5. 무게
        int weight = readInt("택배 무게를 입력하세요 : ");

        // 6. 배송 종류
        DeliveryType deliveryType = readDeliveryType();

        // 7. 택배 접수일
        String registeredDate = readLine("택배 접수일을 입력하세요 (예: 2026-09-09) : ");

        Parcel parcel = new Parcel(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                deliveryType,
                weight,
                ParcelStatus.접수,
                registeredDate
        );
        parcels[parcelCount] = parcel;
        parcelCount++;

        // 8. 배송비
        parcel.fees = calculateFee(parcel);
        // 9. 배송예정일자
        parcel.expectedDeliveryDates = calculateExpectedDeliveryDays(parcel.deliveryType);

        addHistory(parcel, ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        System.out.println("접수가 완료되었습니다. 운송장 번호 : " + trackingNumber);

    }

    // 정해진 배송 종류 중 하나를 입력받은 뒤, 배송 타입으로 변환하여 반환한다.
    static DeliveryType readDeliveryType() {
        while (true) {
            // 6. 배송 종류
            String deliveryTypeString = readLine("배송 종류를 입력하세요(일반/특급/냉장/해외) : ");

            // 6-1. 배송 종류 변환
            if(deliveryTypeString.equals("일반")) {
                return DeliveryType.일반;
            } else if(deliveryTypeString.equals("특급")) {
                return DeliveryType.특급;
            } else if(deliveryTypeString.equals("냉장")) {
                return DeliveryType.냉장;
            } else if(deliveryTypeString.equals("해외")) {
                return DeliveryType.해외;
            }

            // 오타 처리
            System.out.println("배송 종류가 올바르지 않습니다.");
        }
    }

    // 배송 종류별 예상 도착 일수를 계산한다.
    static int calculateExpectedDeliveryDays(DeliveryType deliveryType) {
        // 배송일 계산
        int expectedDays = 3; // 기본 예상 배송일에서 배송 종류에 따라 변경할 변수
        if (deliveryType == DeliveryType.특급 || deliveryType == DeliveryType.냉장) {
            expectedDays = 1;
        } else if (deliveryType == DeliveryType.해외) {
            expectedDays = 7;
        }

        return expectedDays;
    }
    // 배송비 계산 메서드
    static int calculateFee(Parcel parcel) {
        int deliveryFee = 3000;
        if (parcel.weight >= 3){ // 무게가 3kg 이상인 경우
            deliveryFee += 2000;
        }

        if (parcel.destination.equals("제주")){
            deliveryFee += 3000;
        }

        if (parcel.deliveryType == DeliveryType.특급) {
            deliveryFee += 2000;
        } else if (parcel.deliveryType == DeliveryType.냉장) {
            deliveryFee += 4000;
        } else if (parcel.deliveryType == DeliveryType.해외) {
            deliveryFee += 15000;
        }

        return deliveryFee;
    }
    // 택배의 배송 이력 배열에 한 건을 추가한다.
    static void addHistory(Parcel parcel,
                                   ParcelStatus beforeParcelStatus,
                                   ParcelStatus afterParcelStatus,
                                   String historyChangedDate) {
        // 히스토리
        DeliveryHistory deliveryHistory = new DeliveryHistory(
                parcel.trackingNumbers,
                beforeParcelStatus,
                afterParcelStatus,
                historyChangedDate
        );
        parcel.histories[parcel.historyCount] = deliveryHistory;
        parcel.historyCount++;
    }
    // 택배 조회
    static void findParcel() {
        // 1. 사용자로부터 운송장 번호 입력받기
        String searchTarget = readLine("운송장 번호를 입력하세요 : ");

        Parcel parcel = findParcelByTrackingNumber(searchTarget);
        // 3. 못 찾았으면 못 찾겠다 하기
        if(parcel == null){
            System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
            return;
        }

        // 조회된 택배 상세 정보 출력
        printParcel(parcel);
    }
    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        System.out.println("운송장 번호: " + parcel.trackingNumbers);
        System.out.println("수령인: " + parcel.receiverNames);
        System.out.println("연락처: " + parcel.receiverPhoneNumbers);
        System.out.println("배송 지역: " + parcel.destination);
        System.out.println("배송 종류: " + parcel.deliveryType);
        System.out.println("무게: " + parcel.weight + "kg");
        System.out.println("배송비: " + parcel.fees + "원");
        System.out.println("상태: " + parcel.parcelStatus);
        System.out.println("접수일: " + parcel.registeredDates);
        System.out.println("예상 도착: " + parcel.expectedDeliveryDates + "일 후");
    }
    // 전체 택배 조회
    static void printAllParcels() {
        if (parcelCount == 0) {
            System.out.println("접수된 택배가 없습니다.");
            return;
        }

        for (int index = 0; index < parcelCount; index++) {
            System.out.println(
                    parcels[index].trackingNumbers + " / "
                            + parcels[index].receiverNames + " / "
                            + parcels[index].deliveryType + " / "
                            + parcels[index].parcelStatus
            );
        }
    }
    // 출고 처리
    static void shipParcel() {
        // 1. 출고할 운송장 번호 입력받기
        String searchTarget = readLine("출고할 운송장 번호 : ");

        // 2. Parcel 배열에서, 해당 운송장 번호와 일치하는 데이터 찾기
        Parcel parcel = findParcelByTrackingNumber(searchTarget);

        if(parcel == null){
            System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
            return;
        }

        if (parcel.parcelStatus != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 출고할 수 있습니다.");
            return;
        }

        // 2. 접수 -> 출고 상태 변경
        String shippedDate = readLine("출고 날짜를 입력하세요 (예 2026-09-09) :");

        parcel.parcelStatus = ParcelStatus.출고;

        // 3. 히스토리 추가 (접수 -> 출고)
        addHistory(parcel, ParcelStatus.접수, ParcelStatus.출고, shippedDate);

        // [예외 상황]
        // - 운송장 번호 존재하지 않을 수 있음
        // - "접수" 상태가 아닐 수 있음.
    }
    // 취소 처리
    static void cancelParcel() {
        // 1. 출고할 운송장 번호 입력받기
        String searchTarget = readLine("취소할 운송장 번호 : ");
        Parcel parcel = findParcelByTrackingNumber(searchTarget);

        if(parcel == null){
            System.out.println("해당 운송장 번호를 찾을 수 없습니다.");
            return;
        }

        if (parcel.parcelStatus != ParcelStatus.접수) {
            System.out.println("접수 상태의 택배만 취소할 수 있습니다.");
            return;
        }

        // 2. 접수 -> 취소 상태 변경
        String shippedDate = readLine("취소 날짜를 입력하세요 (예 2026-09-09) :");

        parcel.parcelStatus = ParcelStatus.취소;

        // 3. 히스토리 추가 (접수 -> 취소)
        addHistory(parcel, ParcelStatus.접수, ParcelStatus.취소, shippedDate);
    }

    // 배송 이력 출력
    static void printHistory() {
        // 운송장 번호 입력받아서
        String searchTarget = readLine("조회할 운송장 번호 : ");

        Parcel parcel = findParcelByTrackingNumber(searchTarget);

        // history 에 해당 운송장번호로 기록된 내역을 모두 출력한다.
        for(int i=0; i<parcel.historyCount; i++){
            System.out.println(
                    parcel.histories[i].historyTrackingNumbers + "|"
                            + parcel.histories[i].historyChangedDates + "|"
                            + parcel.histories[i].beforeParcelStatus + "|"
                            + parcel.histories[i].afterParcelStatus
            );
        }
    }

    // 운송장 번호가 일치하는 택배 객체를 찾아 반환한다.
    static Parcel findParcelByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcelCount; index++) {
            if (parcels[index].trackingNumbers.equals(trackingNumber)) {
                return parcels[index];
            }
        }

        return null;
    }
}

class Parcel {
    final String trackingNumbers;
    String receiverNames;
    String receiverPhoneNumbers;
    String destination;
    final DeliveryType deliveryType;
    int weight;
    int fees;
    ParcelStatus parcelStatus;
    final String registeredDates;
    int expectedDeliveryDates;

    DeliveryHistory[] histories = new DeliveryHistory[20];
    int historyCount = 0;

    public Parcel(
            String trackingNumbers,
            String receiverNames,
            String receiverPhoneNumbers,
            String destination,
            DeliveryType deliveryType,
            int weight,
            ParcelStatus parcelStatus,
            String registeredDates
    ) {
        this.trackingNumbers = trackingNumbers;
        this.receiverNames = receiverNames;
        this.receiverPhoneNumbers = receiverPhoneNumbers;
        this.destination = destination;
        this.deliveryType = deliveryType;
        this.weight = weight;
        this.parcelStatus = parcelStatus;
        this.registeredDates = registeredDates;
    }
}

class DeliveryHistory {
    String historyTrackingNumbers;
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    String historyChangedDates;

    public DeliveryHistory (
            String historyTrackingNumbers,
            ParcelStatus beforeParcelStatus,
            ParcelStatus afterParcelStatus,
            String historyChangedDates
    ) {
        this.historyTrackingNumbers = historyTrackingNumbers;
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDates = historyChangedDates;
    }
}
