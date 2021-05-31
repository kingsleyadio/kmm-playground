import SwiftUI
import Shared

struct ContentView: View {
    let greet = Greeting().greeting(name: "Kingsley")
    let counter = Counter()
    
    @State var state: Int32 = 0
    
    func increment() {
        counter.increment()
        state = counter.state()
    }
    
    func decrement() {
        counter.decrement()
        state = counter.state()
    }
    
    var body: some View {
        
        VStack {

            Text(greet)
                .padding()
            
            Button("Increment me", action: increment)
            Button("Decrement me", action: decrement)
            
            Text("Current value: " + String(state))
                .padding()

        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
