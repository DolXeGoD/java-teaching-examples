import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

public class Ch15Collections {
    static Scanner scanner = new Scanner(System.in);

    static ParcelRepository parcelRepository =
            new MemoryParcelRepository();

    public static void main(String[] args) {
        while (true) {
            try{
                printMenu();
                int menu = readInt("메뉴 선택 : ");

                switch (menu) {
                    case 0:
                        if(!parcelRepository.isEmpty()) {
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
                        changeParcelStatus(ParcelStatus.출고);
                        break;
                    case 5: // 취소 처리 (접수 상태만)
                        changeParcelStatus(ParcelStatus.취소);
                        break;
                    case 6: // 이력 조회
                        printHistory();
                        break;
                    default: // 메뉴 잘못 선택한 경우
                        System.out.println("없는 메뉴입니다. 다시 선택해주세요.");
                        break;

                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
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
    static void registerParcel() throws ParcelException {
        // 각각의 정보를 받아서 배열에 넣어주기
        // 1. 운송장 번호
        String trackingNumber = readLine("운송장 번호를 입력하세요 : ");
        // 2. 수령인 이름
        String receiverName = readLine("수령인 이름 입력하세요 : ");
        // 3. 수령인 연락처
        String receiverPhoneNumber = readLine("수령인 연락처를 입력하세요 : ");
        // 4. 배송 지역
        String destination = readLine("배송 지역을 입력하세요 : ");

        // 5. 택배 접수일
        LocalDateTime registeredDate = LocalDateTime.now();

        System.out.println("1. 간편 접수(일반 배송, 1kg)");
        System.out.println("2. 상세 접수(배송 종류와 무게 직접 입력)");
        int registerType = readInt("접수 방식: ");

        Parcel parcel;
        if (registerType == 1) {
            // 간편접수용 택배 생성
            // 무게, 택배 종류 생략된 간편용 생성자 호출
            parcel = new NormalParcel(
                    trackingNumber,
                    receiverName,
                    receiverPhoneNumber,
                    destination,
                    registeredDate
            );
        } else if(registerType == 2) {
            // 무게
            int weight = readInt("택배 무게를 입력하세요 : ");

            if (weight > Parcel.MAXIMUM_WEIGHT) {
                throw new ParcelException("택배 무게는 20kg을 넘을 수 없습니다.");
            }

            // 배송 종류
            DeliveryType deliveryType = readDeliveryType();
            if(deliveryType == DeliveryType.일반){
                parcel = new NormalParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.특급) {
                parcel = new ExpressParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.냉장) {
                parcel = new RefrigeratedParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else if (deliveryType == DeliveryType.해외){
                parcel = new OverseasParcel(
                        trackingNumber,
                        receiverName,
                        receiverPhoneNumber,
                        destination,
                        weight,
                        registeredDate
                );
            } else {
                throw new ParcelException("잘못된 배송 타입입니다.");
            }

        } else {
            throw new ParcelException("접수 방식을 다시 선택해주세요.");
        }
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수, registeredDate);
        parcelRepository.save(parcel);

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

    // 택배 조회
    static void findParcel() throws ParcelException {
        // 1. 사용자로부터 운송장 번호 입력받기
        String searchTarget = readLine("운송장 번호를 입력하세요 : ");

        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);
        // 3. 못 찾았으면 못 찾겠다 하기
        if(parcel == null){
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        // 조회된 택배 상세 정보 출력
        printParcel(parcel);
    }

    // 한 택배의 상세 정보를 출력한다.
    static void printParcel(Parcel parcel) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("운송장 번호: ").append(parcel.getTrackingNumber()).append('\n');
        stringBuilder.append("수령인: ").append(parcel.getReceiverName()).append('\n');
        stringBuilder.append("연락처: ").append(parcel.getReceiverPhoneNumber()).append('\n');
        stringBuilder.append("배송 지역: ").append(parcel.getDestination()).append('\n');
        stringBuilder.append("배송 종류: ").append(parcel.getDeliveryType()).append('\n');
        stringBuilder.append("무게: ").append(parcel.getWeight()).append("kg\n");
        stringBuilder.append("배송비: ").append(parcel.getFee()).append("원\n");
        stringBuilder.append("상태: ").append(parcel.getParcelStatus()).append('\n');
        stringBuilder.append("접수일: ").append(parcel.getRegisteredDate()).append('\n');
        stringBuilder.append("예상 도착일: ").append(parcel.getExpectedDeliveryDate()).append('\n');
        System.out.println(stringBuilder);
    }

    // 전체 택배 조회
    static void printAllParcels() throws ParcelException {
        if (parcelRepository.isEmpty()) {
            throw new ParcelException("접수된 택배가 없습니다.");
        }

        List<Parcel> parcels = parcelRepository.findAll();

        for (Parcel parcel : parcels) {
            System.out.println(
                    parcel.getTrackingNumber() + " / "
                            + parcel.getReceiverName() + " / "
                            + parcel.getDeliveryType() + " / "
                            + parcel.getParcelStatus()
            );
        }
    }

    // 출고/취소 통합 메서드
    // 접수 상태의 택배를 출고 또는 취소 상태로 변경 후 이력 추가하는 메서드
    static void changeParcelStatus(ParcelStatus afterParcelStatus)
        throws ParcelException {
        String trackingNumber = readLine(afterParcelStatus + "할 운송장 번호: ");
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);

        if (parcel == null){
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        if (parcel.getParcelStatus() != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 " + afterParcelStatus + " 처리할 수 있습니다.");
        }

        // 택배 상태 변경 전에 이력을 추가해주어야
        // 택배의 기존 상태, 신규 상태가 이력에 잘 저장됩니다.
        parcel.addHistory(parcel.getParcelStatus(), afterParcelStatus, LocalDateTime.now());
        parcel.setParcelStatus(afterParcelStatus);
        parcelRepository.save(parcel);
        System.out.println(afterParcelStatus + " 처리했습니다.");
    }

    // 배송 이력 출력
    static void printHistory() throws ParcelException {
        // 운송장 번호 입력받아서
        String searchTarget = readLine("조회할 운송장 번호 : ");
        Parcel parcel = parcelRepository.findByTrackingNumber(searchTarget);

        if (parcel == null) {
            throw new ParcelException("해당 택배를 찾을 수 없습니다.");
        }

        // history 에 해당 운송장번호로 기록된 내역을 모두 출력한다.
        for(int i=0; i<parcel.getHistoryCount(); i++){
            System.out.println(
                    parcel.getTrackingNumber() + "|"
                            + parcel.getHistories()[i].historyChangedDates + "|"
                            + parcel.getHistories()[i].beforeParcelStatus + "|"
                            + parcel.getHistories()[i].afterParcelStatus
            );
        }
    }
}

interface ParcelRepository {
    // 택배를 저장하거나, 기존 택배 정보를 덮어씌운다
    void save(Parcel parcel);

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber);

    // 저장된 택배 전체를 반환한다
    List<Parcel> findAll();

    // 택배 상자 리스트가 비었는지 확인
    boolean isEmpty();
}

class MemoryParcelRepository implements ParcelRepository {
    private List<Parcel> parcels = new ArrayList<>();

    @Override
    public void save(Parcel parcel) {
        if(findByTrackingNumber(parcel.getTrackingNumber()) == null){
            parcels.add(parcel);
        }
    }

    @Override
    public Parcel findByTrackingNumber(String trackingNumber) {
        for(Parcel parcel : parcels){
            if (parcel.getTrackingNumber().equals(trackingNumber)) {
                return parcel;
            }
        }

        return null;
    }

    @Override
    public List<Parcel> findAll() {
        return parcels;
    }

    @Override
    public boolean isEmpty() {
        return parcels.isEmpty();
    }
}

abstract class Parcel {
    // static final - 상수 (특정 객체에 소속되지도, 변경되지도 않는 항상 일정한 값)
    // static - 특정 객체가 아닌 클래스 자체에 소속되는 값/메서드
    // final - 불변값 (특정 객체에 소속되지만, 값이 정해진 이후로는 변경되지 않는 값)
    static final int MAXIMUM_WEIGHT = 20;
    private static final int BASIC_DELIVERY_FEE = 3000;
    private static final int JEJU_DELIVERY_FEE = 3000;
    private static final int HEAVY_PARCEL_FEE = 2000;
    private static final int HEAVY_PARCEL_MINIMUM_WEIGHT = 3;

    private final String trackingNumber;
    private String receiverName;
    private String receiverPhoneNumber;
    private String destination;
    private int weight;
    private int fee;
    private ParcelStatus parcelStatus;
    private final LocalDateTime registeredDate;
    private LocalDateTime expectedDeliveryDate;
    private DeliveryType deliveryType;
    private DeliveryHistory[] histories = new DeliveryHistory[20];
    private int historyCount = 0;

    Parcel(String trackingNumber,
           String receiverName,
           String receiverPhoneNumber,
           String destination,
           int weight,
           LocalDateTime registeredDate,
           DeliveryType deliveryType
    ) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        setWeight(weight);
        this.registeredDate = registeredDate;
        this.parcelStatus = ParcelStatus.접수;
        this.deliveryType = deliveryType;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    public String getDestination() {
        return destination;
    }

    public int getWeight() {
        return weight;
    }

    public int getFee() {
        return fee;
    }

    public ParcelStatus getParcelStatus() {
        return parcelStatus;
    }

    public LocalDateTime getRegisteredDate() {
        return registeredDate;
    }

    public LocalDateTime getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public DeliveryHistory[] getHistories() {
        return histories;
    }

    private boolean setWeight(int weight) {
        if (weight > MAXIMUM_WEIGHT) {
            return false;
        }
        this.weight = weight;
        return true;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setParcelStatus(ParcelStatus parcelStatus) {
        this.parcelStatus = parcelStatus;
    }

    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }

    // 배송비 계산 메서드
    int calculateBaseFee() {
        int deliveryFee = BASIC_DELIVERY_FEE;
        if (getWeight() >= HEAVY_PARCEL_MINIMUM_WEIGHT){ // 무게가 3kg 이상인 경우
            deliveryFee += HEAVY_PARCEL_FEE;
        }

        if (getDestination().equals("제주")){
            deliveryFee += JEJU_DELIVERY_FEE;
        }

        return deliveryFee;
    }

    // 택배의 배송 이력 배열에 한 건을 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus,
                           ParcelStatus afterParcelStatus,
                           LocalDateTime historyChangedDate) {
        // 히스토리
        DeliveryHistory deliveryHistory = new DeliveryHistory(
                beforeParcelStatus,
                afterParcelStatus,
                historyChangedDate
        );

        // 클래스 내부에선 private 필드/메서드 모두 사용 가능
        histories[historyCount] = deliveryHistory;
        historyCount++;
    }
}

class NormalParcel extends Parcel {
    // 필드 X

    // 간편 접수용 생성자
    public NormalParcel(
            String trackingNumber,
            String receiverName,
            String receiverPhoneNumber,
            String destination,
            LocalDateTime registeredDate
    ) {
        this(trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                1,
                registeredDate);
    }

    // 일반(상세) 접수용 생성자
    public NormalParcel(
            String trackingNumber,
            String receiverName,
            String receiverPhoneNumber,
            String destination,
            int weight,
            LocalDateTime registeredDate
    ) {
        super(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                weight,
                registeredDate,
                DeliveryType.일반
        );

        // 배송비 & 배송 예정 일자 세팅
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }

    // 메서드
    // 1. 일반 택배 배송비 계산
    int calculateFee() {
        return calculateBaseFee();
    }

    // 2. 예상 도착일수 반환
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(3);
    }
}

// 특급 배송 클래스
class ExpressParcel extends Parcel {
    // 생성자
    ExpressParcel(
            String trackingNumber,
            String receiverName,
            String receiverPhoneNumber,
            String destination,
            int weight,
            LocalDateTime registeredDate
    ){
        super(
                trackingNumber,
                receiverName,
                receiverPhoneNumber,
                destination,
                weight,
                registeredDate,
                DeliveryType.특급
        );

        // 배송비 & 배송 예정 일자 세팅
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }

    // 메서드
    // 1. 배송비 계산
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }

    // 2. 예상 소요일 반환
    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(1);
    }
}

class RefrigeratedParcel extends Parcel {
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.냉장);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }

    int calculateFee() {
        return calculateBaseFee() + 4000;
    }

    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(1);
    }
}

class OverseasParcel extends Parcel {
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDateTime registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination,
                weight, registeredDate, DeliveryType.해외);
        setFee(calculateFee());
        setExpectedDeliveryDate(calculateExpectedDeliveryDays());
    }

    int calculateFee() {
        return calculateBaseFee() + 15000;
    }

    LocalDateTime calculateExpectedDeliveryDays() {
        return getRegisteredDate().plusDays(7);
    }
}

class DeliveryHistory {
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    LocalDateTime historyChangedDates;

    public DeliveryHistory (
            ParcelStatus beforeParcelStatus,
            ParcelStatus afterParcelStatus,
            LocalDateTime historyChangedDates
    ) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.historyChangedDates = historyChangedDates;
    }
}

// 예외 클래스
class ParcelException extends Exception {
    // 생성자
    ParcelException(String message) {
        super(message);
    }
}
