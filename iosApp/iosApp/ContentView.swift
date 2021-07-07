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
    var closeable: Closeable?

    init() {
        closeable = flowCounter.wrap().collect { [weak self] kInt in
            self?.state = kInt!.int32Value
        }
    }

    func increment() {
        flowCounter.increment()
    }

    func decrement() {
        flowCounter.decrement()
    }

    deinit {
        closeable?.close()
    }
}