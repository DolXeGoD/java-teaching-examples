import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 택배의 현재 상태와 배송 이력에 기록할 상태다.
enum ParcelStatus {
    없음,
    접수,
    출고,
    취소
}

public class Ch15Collections {
    // 컬렉션으로 바뀐 저장소를 실행해 보는 예시다.
    public static void main(String[] args) {
        ParcelService parcelService = new ParcelService(new MemoryParcelRepository());

        try {
            parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2, "일반");
            parcelService.register("1002", "김영희", "010-3333-4444", "제주", 4, "냉장");
            parcelService.changeStatus("1002", ParcelStatus.출고);
            parcelService.printAllParcels();
            parcelService.printHistory("1002");
        } catch (ParcelException exception) {
            System.out.println(exception.getMessage());
        }
    }
}

// 택배 업무 규칙을 처리하는 클래스다.
class ParcelService {
    private ParcelRepository parcelRepository;

    // 업무 처리에 사용할 저장소를 받는다.
    ParcelService(ParcelRepository parcelRepository) {
        this.parcelRepository = parcelRepository;
    }

    // 새 택배를 접수하고 최초 이력을 남긴다.
    void register(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, String deliveryType) throws ParcelException {
        if (parcelRepository.findByTrackingNumber(trackingNumber) != null) {
            throw new ParcelException("이미 사용 중인 운송장 번호입니다.");
        }

        Parcel parcel = createParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, deliveryType, LocalDate.now());
        parcel.expectedDeliveryDate = parcel.registeredDate.plusDays(parcel.getExpectedDeliveryDays());
        parcel.addHistory(ParcelStatus.없음, ParcelStatus.접수);
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, ParcelStatus afterParcelStatus) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        if (parcel.parcelStatus != ParcelStatus.접수) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.parcelStatus, afterParcelStatus);
        parcel.parcelStatus = afterParcelStatus;
        parcelRepository.save(parcel);
    }

    // 저장된 모든 택배의 요약 정보를 출력한다.
    void printAllParcels() {
        for (Parcel parcel : parcelRepository.findAll()) {
            System.out.println(parcel.trackingNumber + " / " + parcel.receiverName
                    + " / " + parcel.getDeliveryType() + " / " + parcel.parcelStatus);
        }
    }

    // 특정 택배의 배송 이력을 출력한다.
    void printHistory(String trackingNumber) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        for (DeliveryHistory history : parcel.histories) {
            System.out.println(history.changedAt + " / " + history.beforeParcelStatus
                    + " → " + history.afterParcelStatus);
        }
    }

    // 운송장 번호로 택배를 찾고 없으면 예외를 발생시킨다.
    Parcel findParcel(String trackingNumber) throws ParcelException {
        Parcel parcel = parcelRepository.findByTrackingNumber(trackingNumber);
        if (parcel == null) {
            throw new ParcelException("존재하지 않는 운송장 번호입니다.");
        }

        return parcel;
    }

    // 배송 종류에 맞는 택배 객체를 만든다.
    Parcel createParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                        String destination, int weight, String deliveryType,
                        LocalDate registeredDate) {
        if (deliveryType.equals("특급")) {
            Parcel parcel = new ExpressParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("냉장")) {
            Parcel parcel = new RefrigeratedParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        if (deliveryType.equals("해외")) {
            Parcel parcel = new OverseasParcel(trackingNumber, receiverName,
                    receiverPhoneNumber, destination, weight, registeredDate);
            return parcel;
        }

        Parcel parcel = new NormalParcel(trackingNumber, receiverName, receiverPhoneNumber,
                destination, weight, registeredDate);
        return parcel;
    }
}

// 택배 업무 처리에 실패했을 때 사용하는 예외 클래스다.
class ParcelException extends Exception {
    // 오류 원인을 예외 메시지로 전달한다.
    ParcelException(String message) {
        super(message);
    }
}

// 택배 저장과 조회 기능을 약속하는 인터페이스다.
interface ParcelRepository {
    // 택배를 저장하거나 갱신한다.
    void save(Parcel parcel);

    // 운송장 번호로 택배를 찾는다.
    Parcel findByTrackingNumber(String trackingNumber);

    // 저장된 모든 택배를 반환한다.
    List<Parcel> findAll();
}

// ========== CH15 변경 ==========
// 배열 저장소를 List와 Map으로 바꾼다.
// =================================
class MemoryParcelRepository implements ParcelRepository {
    private List<Parcel> parcelList = new ArrayList<>();
    private Map<String, Parcel> parcelMap = new HashMap<>();

    // 새 택배를 목록과 운송장 번호 Map에 함께 저장한다.
    public void save(Parcel parcel) {
        if (!parcelMap.containsKey(parcel.trackingNumber)) {
            parcelList.add(parcel);
            parcelMap.put(parcel.trackingNumber, parcel);
        }
    }

    // Map에서 운송장 번호로 택배를 바로 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        return parcelMap.get(trackingNumber);
    }

    // 전체 택배 목록을 반환한다.
    public List<Parcel> findAll() {
        return parcelList;
    }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    ParcelStatus parcelStatus = ParcelStatus.접수;
    LocalDate registeredDate;
    LocalDate expectedDeliveryDate;

    // ========== CH15 변경 ==========
    // 직접 만든 MemoryStorage 대신 ArrayList를 사용한다.
    // =================================
    List<DeliveryHistory> histories = new ArrayList<>();

    // 공통 택배 정보를 초기화한다.
    Parcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
           String destination, int weight, LocalDate registeredDate) {
        this.trackingNumber = trackingNumber;
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.destination = destination;
        this.weight = weight;
        this.registeredDate = registeredDate;
    }

    // 상태 변경 이력을 목록에 추가한다.
    void addHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus) {
        histories.add(new DeliveryHistory(beforeParcelStatus, afterParcelStatus, LocalDateTime.now()));
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int deliveryFee = 3000;
        if (weight >= 3) {
            deliveryFee += 2000;
        }
        if (destination.equals("제주")) {
            deliveryFee += 3000;
        }
        return deliveryFee;
    }
}

// 일반 배송 규칙을 가진 클래스다.
class NormalParcel extends Parcel {
    // 일반 배송 택배를 초기화한다.
    NormalParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                 String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 일반 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee();
    }
    // 일반 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 3;
    }
    // 일반 배송 이름을 반환한다.
    String getDeliveryType() {
        return "일반";
    }
}

// 특급 배송 규칙을 가진 클래스다.
class ExpressParcel extends Parcel {
    // 특급 배송 택배를 초기화한다.
    ExpressParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                  String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 특급 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 2000;
    }
    // 특급 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 특급 배송 이름을 반환한다.
    String getDeliveryType() {
        return "특급";
    }
}

// 냉장 배송 규칙을 가진 클래스다.
class RefrigeratedParcel extends Parcel {
    // 냉장 배송 택배를 초기화한다.
    RefrigeratedParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                       String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 냉장 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 4000;
    }
    // 냉장 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 1;
    }
    // 냉장 배송 이름을 반환한다.
    String getDeliveryType() {
        return "냉장";
    }
}

// 해외 배송 규칙을 가진 클래스다.
class OverseasParcel extends Parcel {
    // 해외 배송 택배를 초기화한다.
    OverseasParcel(String trackingNumber, String receiverName, String receiverPhoneNumber,
                   String destination, int weight, LocalDate registeredDate) {
        super(trackingNumber, receiverName, receiverPhoneNumber, destination, weight, registeredDate);
    }
    // 해외 배송비를 계산한다.
    int calculateFee() {
        return calculateBaseFee() + 15000;
    }
    // 해외 배송 일수를 반환한다.
    int getExpectedDeliveryDays() {
        return 7;
    }
    // 해외 배송 이름을 반환한다.
    String getDeliveryType() {
        return "해외";
    }
}

// 상태 변경 시각까지 보관하는 배송 이력 클래스다.
class DeliveryHistory {
    ParcelStatus beforeParcelStatus;
    ParcelStatus afterParcelStatus;
    LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(ParcelStatus beforeParcelStatus, ParcelStatus afterParcelStatus, LocalDateTime changedAt) {
        this.beforeParcelStatus = beforeParcelStatus;
        this.afterParcelStatus = afterParcelStatus;
        this.changedAt = changedAt;
    }
}
