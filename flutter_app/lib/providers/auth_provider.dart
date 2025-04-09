import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthProvider with ChangeNotifier {
  final String baseUrl = 'http://localhost:8080/rest';
  final storage = const FlutterSecureStorage();
  String? _token;
  String? _username;
  String? _role;

  String? get token => _token;
  String? get username => _username;
  String? get role => _role;
  bool get isAuthenticated => _token != null;

  Future<bool> login(String username, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/login'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'username': username,
          'password': password,
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        _token = data['token'];
        _username = username;
        _role = data['role'];
        await storage.write(key: 'token', value: _token);
        await storage.write(key: 'username', value: _username);
        await storage.write(key: 'role', value: _role);
        notifyListeners();
        return true;
      }
      return false;
    } catch (e) {
      print('Login error: $e');
      return false;
    }
  }

  Future<bool> register(Map<String, dynamic> userData) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/register'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode(userData),
      );

      return response.statusCode == 200;
    } catch (e) {
      print('Registration error: $e');
      return false;
    }
  }

  Future<void> logout() async {
    _token = null;
    _username = null;
    _role = null;
    await storage.delete(key: 'token');
    await storage.delete(key: 'username');
    await storage.delete(key: 'role');
    notifyListeners();
  }

  Future<void> checkAuthStatus() async {
    _token = await storage.read(key: 'token');
    _username = await storage.read(key: 'username');
    _role = await storage.read(key: 'role');
    notifyListeners();
  }
} 