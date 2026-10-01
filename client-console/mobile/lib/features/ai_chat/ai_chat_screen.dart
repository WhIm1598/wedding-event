import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/app_notification.dart';
import '../../data/models/wedding_plan.dart';
import '../../data/repositories/plan_repository.dart';
import '../../state/session_controller.dart';

/// FN-PLAN-03: AI wedding planner assistant (in-house model via client backend)
class AiChatScreen extends StatefulWidget {
  const AiChatScreen({super.key});

  @override
  State<AiChatScreen> createState() => _AiChatScreenState();
}

class _AiChatScreenState extends State<AiChatScreen> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final List<ChatMessage> _messages = [
    const ChatMessage(id: 'm-0', text: 'Chào bạn! Mình là AI WedPlanner. Ngân sách dự kiến cho đám cưới của bạn là bao nhiêu?', fromUser: false),
  ];
  bool _typing = false;
  bool _generating = false;

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) _scroll.animateTo(_scroll.position.maxScrollExtent, duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
    });
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty) return;
    _input.clear();
    setState(() {
      _messages.add(ChatMessage(id: 'u-${_messages.length}', text: text, fromUser: true));
      _typing = true;
    });
    _scrollToEnd();
    final reply = await context.read<PlanRepository>().askAssistant(text);
    if (!mounted) return;
    setState(() {
      _messages.add(ChatMessage(id: 'a-${_messages.length}', text: reply, fromUser: false));
      _typing = false;
    });
    _scrollToEnd();
  }

  /// Extracts a budget like "250 triệu" / "250tr" from the conversation (best effort; the AI service parses the full prompt).
  int? _extractBudget(String prompt) {
    final match = RegExp(r'(\d+)\s*(triệu|tr)').firstMatch(prompt.toLowerCase());
    return match == null ? null : int.parse(match.group(1)!) * 1000000;
  }

  String get _userPrompt => _messages.where((m) => m.fromUser).map((m) => m.text).join('\n');

  Future<void> _generate() async {
    final prompt = _userPrompt;
    setState(() => _generating = true);
    final session = context.read<SessionController>();
    try {
      await context.read<PlanRepository>().generateWithAi(
            AiPlanRequest(prompt: prompt, totalBudget: _extractBudget(prompt), style: WeddingStyle.elegant),
          );
      session.markPlanCreated();
      if (mounted) context.go(Routes.plan);
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _generating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final canGenerate = _messages.length > 2;
    final detectedBudget = _extractBudget(_userPrompt);
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(
            title: 'Trợ lý AI',
            subtitle: Text('Đang hoạt động', style: TextStyle(fontSize: 12, color: AppColors.emerald500, fontWeight: FontWeight.w500)),
          ),
          Expanded(
            child: ListView(
              controller: _scroll,
              padding: const EdgeInsets.all(16),
              children: [
                for (final m in _messages) _Bubble(message: m),
                if (_typing) const _Bubble(message: ChatMessage(id: 'typing', text: '...', fromUser: false)),
                if (canGenerate)
                  Padding(
                    padding: const EdgeInsets.only(top: 24),
                    child: Center(
                      child: DecoratedBox(
                        decoration: BoxDecoration(gradient: AppColors.aiGradient, borderRadius: BorderRadius.circular(12)),
                        child: TextButton.icon(
                          onPressed: _generating ? null : _generate,
                          style: TextButton.styleFrom(
                            foregroundColor: AppColors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                          ),
                          icon: _generating
                              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.white))
                              : const Icon(Icons.auto_awesome, size: 18),
                          label: const Text('Sinh Kế Hoạch', style: TextStyle(fontWeight: FontWeight.bold)),
                        ),
                      ),
                    ),
                  ),
                if (canGenerate && detectedBudget != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      'Ngân sách nhận diện: ${Fmt.vnd(detectedBudget)}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 11, color: AppColors.slate400),
                    ),
                  ),
              ],
            ),
          ),
          BottomActionBar(
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _input,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => _send(),
                    decoration: InputDecoration(
                      hintText: 'Nhập câu trả lời...',
                      fillColor: AppColors.slate100,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(999), borderSide: BorderSide.none),
                      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(999), borderSide: BorderSide.none),
                      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(999), borderSide: BorderSide.none),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  style: IconButton.styleFrom(backgroundColor: AppColors.pink600, minimumSize: const Size(48, 48)),
                  onPressed: _send,
                  icon: const Icon(Icons.send, size: 18, color: AppColors.white),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final user = message.fromUser;
    return Align(
      alignment: user ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        constraints: BoxConstraints(maxWidth: MediaQuery.sizeOf(context).width * 0.8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: user ? AppColors.pink600 : AppColors.white,
          border: user ? null : Border.all(color: AppColors.slate200),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(user ? 16 : 0),
            bottomRight: Radius.circular(user ? 0 : 16),
          ),
        ),
        child: Text(message.text, style: TextStyle(fontSize: 14, color: user ? AppColors.white : AppColors.slate800)),
      ),
    );
  }
}
