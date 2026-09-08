import java.time.LocalDate;
import java.time.LocalDateTime;

public class Ch13Generics {
    // 제네릭 저장소가 택배와 배송 이력에 모두 쓰이는지 확인한다.
    public static void main(String[] args) {
        ParcelService parcelService = new ParcelService(new MemoryParcelRepository());

        try {
            parcelService.register("1001", "홍길동", "010-1111-2222", "서울", 2, "일반");
            parcelService.changeStatus("1001", "출고");
            parcelService.printHistory("1001");
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
        parcel.addHistory("없음", "접수");
        parcelRepository.save(parcel);
    }

    // 접수 상태의 택배를 출고 또는 취소 상태로 바꾼다.
    void changeStatus(String trackingNumber, String afterStatus) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);
        if (!parcel.status.equals("접수")) {
            throw new ParcelException("접수 상태의 택배만 처리할 수 있습니다.");
        }

        parcel.addHistory(parcel.status, afterStatus);
        parcel.status = afterStatus;
        parcelRepository.save(parcel);
    }

    // 특정 택배의 배송 이력을 출력한다.
    void printHistory(String trackingNumber) throws ParcelException {
        Parcel parcel = findParcel(trackingNumber);

        for (int index = 0; index < parcel.histories.size(); index++) {
            DeliveryHistory history = parcel.histories.get(index);
            System.out.println(history.changedAt + " / " + history.beforeStatus
                    + " → " + history.afterStatus);
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

// ========== CH13 변경 ==========
// 자료형만 다른 배열 관리 코드를 제네릭 저장소로 묶는다.
// =================================
class MemoryStorage<T> {
    private T[] data;
    private int count = 0;

    // 어떤 자료형 배열을 사용할지 받아 저장소를 만든다.
    MemoryStorage(T[] data) {
        this.data = data;
    }

    // 저장소 끝에 데이터를 추가한다.
    void add(T item) {
        data[count] = item;
        count++;
    }

    // 지정한 위치의 데이터를 반환한다.
    T get(int index) {
        return data[index];
    }

    // 현재 저장된 데이터 개수를 반환한다.
    int size() {
        return count;
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
}

// ========== CH13 변경 ==========
// 택배 배열 대신 MemoryStorage<Parcel>을 사용한다.
// =================================
class MemoryParcelRepository implements ParcelRepository {
    private MemoryStorage<Parcel> parcels = new MemoryStorage<>(new Parcel[100]);

    // 새 택배만 저장소에 추가한다.
    public void save(Parcel parcel) {
        if (findByTrackingNumber(parcel.trackingNumber) == null) {
            parcels.add(parcel);
        }
    }

    // 저장소에서 운송장 번호가 같은 택배를 찾는다.
    public Parcel findByTrackingNumber(String trackingNumber) {
        for (int index = 0; index < parcels.size(); index++) {
            Parcel parcel = parcels.get(index);
            if (parcel.trackingNumber.equals(trackingNumber)) {
                return parcel;
            }
        }
        return null;
    }
}

// 배송 종류가 공유하는 정보와 배송 이력을 가진 부모 클래스다.
abstract class Parcel {
    String trackingNumber;
    String receiverName;
    String receiverPhoneNumber;
    String destination;
    int weight;
    String status = "접수";
    LocalDate registeredDate;
    LocalDate expectedDeliveryDate;

    // ========== CH13 변경 ==========
    // 이력 배열을 MemoryStorage<DeliveryHistory>로 바꾼다.
    // =================================
    MemoryStorage<DeliveryHistory> histories = new MemoryStorage<>(new DeliveryHistory[20]);

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

    // 상태 변경 이력을 제네릭 저장소에 추가한다.
    void addHistory(String beforeStatus, String afterStatus) {
        histories.add(new DeliveryHistory(beforeStatus, afterStatus, LocalDateTime.now()));
    }

    // 배송 종류별 배송비 계산을 자식 클래스에 맡긴다.
    abstract int calculateFee();

    // 배송 종류별 예상 도착 일수 계산을 자식 클래스에 맡긴다.
    abstract int getExpectedDeliveryDays();

    // 배송 종류 이름을 자식 클래스에 맡긴다.
    abstract String getDeliveryType();

    // 지역과 무게에 따른 공통 기본 배송비를 계산한다.
    int calculateBaseFee() {
        int fee = 3000;
        if (weight >= 3) {
            fee += 2000;
        }
        if (destination.equals("제주")) {
            fee += 3000;
        }
        return fee;
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
    String beforeStatus;
    String afterStatus;
    LocalDateTime changedAt;

    // 배송 이력 한 건을 초기화한다.
    DeliveryHistory(String beforeStatus, String afterStatus, LocalDateTime changedAt) {
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.changedAt = changedAt;
    }
}
