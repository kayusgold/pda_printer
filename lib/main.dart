import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform =
      const MethodChannel("com.example.pda_printer/print_text");
  static const platform2 =
      const MethodChannel("com.example.pda_printer/battery");
  static const EventChannel eventChannel =
      EventChannel('com.example.pda_printer/charging');
  static const MethodChannel scanChannel =
      MethodChannel('com.example.pda_printer/scan');

  @override
  void initState() {
    super.initState();
    _getBatteryLevel();
    //this listen to broadcast from native android
    eventChannel.receiveBroadcastStream().listen(_onEvent, onError: _onError);
  }

  void _onEvent(Object event) {
    print("Battery status: ${event == 'charging' ? '' : 'dis'}charging.");
  }

  void _onError(Object error) {
    print('Battery status: unknown.');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Center(
            child: RaisedButton(
              color: Colors.blue,
              textColor: Colors.white,
              onPressed: _printText,
              child: Text("Print Text"),
            ),
          ),
          RaisedButton(
            color: Colors.redAccent,
            textColor: Colors.white,
            onPressed: _scan,
            child: Text("Scan"),
          ),
        ],
      ),
    );
  }

  void _printText() async {
    print("Printing text...");
    String result;
    try {
      result = await platform.invokeMethod(
          "PrintText", "You are printing this text from the flutter code.");
      print(result);
    } catch (e) {
      print(e);
    }
  }

  void _scan() async {
    print("Scanning...");
    try {
      //initialize the scan call
      String result = await scanChannel.invokeMethod("ScanText");
      print("Scan result: $result");
    } catch (e) {
      print(e);
    }
  }

  void _getBatteryLevel() async {
    try {
      int level = await platform2.invokeMethod("getBatteryLevel");
      print("Battery Level: $level%");
    } catch (e) {
      print("Error: $e");
    }
  }
}
