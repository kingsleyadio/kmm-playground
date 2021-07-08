import SwiftUI
import Shared

struct ContentView: View {
    let greet = Greeting().greeting(name: "Kingsley")
    @ObservedObject var consumer = FlowConsumer()

    var body: some View {

        VStack {

            Text(greet)
                    .padding()

            Button("Increment me", action: consumer.increment)
                    .disabled(consumer.state >= 100)
            Button("Decrement me", action: consumer.decrement)
                    .disabled(consumer.state == 0)

            Text("Current value: " + String(consumer.state))
                    .padding()

        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

class FlowConsumer: ObservableObject {

    private let flowCounter = FlowCounter()

    @Published var state: Int32 = 0
    private let compositeCloseable = CompositeCloseable()

    init() {
        var closeable = flowCounter.wrap().collect { [weak self] kInt in
            self?.state = kInt!.int32Value
        }
        CounterKt.closedBy(closeable, composite: compositeCloseable)

        closeable = flowCounter.wrapLastState().run(
            onSuccess: { value in self.state = value!.int32Value },
            onError: { throwable in NSLog("Error occurred: \(throwable)") }
        )
        CounterKt.closedBy(closeable, composite: compositeCloseable)
    }

    func increment() {
        flowCounter.increment()
    }

    func decrement() {
        flowCounter.decrement()
    }

    deinit {
        compositeCloseable.close()
    }
}