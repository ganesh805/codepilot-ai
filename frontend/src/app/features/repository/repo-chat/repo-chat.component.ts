import { Component, OnInit, ElementRef, ViewChild, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AiProvider, ChatMessage } from '../../../core/models/chat.model';
import { ChatService } from '../../../core/services/chat.service';

@Component({
  selector: 'app-repo-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './repo-chat.component.html',
  styleUrls: ['./repo-chat.component.scss']
})
export class RepoChatComponent implements OnInit, AfterViewChecked {
  private route = inject(ActivatedRoute);
  private chatService = inject(ChatService);
  private snackBar = inject(MatSnackBar);

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  repoUuid: string = '';
  inputMessage: string = '';
  selectedProvider: AiProvider = 'GEMINI';
  messages: ChatMessage[] = [];
  loading = false;
  showCitations: Record<string, boolean> = {};

  suggestedQuestions: string[] = [
    'How does authentication and JWT token validation work in this project?',
    'Explain the repository scanning and AST line chunking workflow.',
    'What REST API endpoints are exposed for repository management?'
  ];

  providersList: { id: AiProvider; label: string; icon: string }[] = [
    { id: 'GEMINI', label: 'Google Gemini 1.5 Pro', icon: '🤖' },
    { id: 'OPENAI', label: 'OpenAI GPT-4o', icon: '⚡' },
    { id: 'DEEPSEEK', label: 'DeepSeek-Coder V2', icon: '🚀' },
    { id: 'HYBRID_ENSEMBLE', label: 'Hybrid Ensemble (Gemini + GPT-4o)', icon: '✨' }
  ];

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
    this.messages.push({
      id: 'welcome',
      sender: 'ASSISTANT',
      text: 'Hello! I am your Multi-Model AI CodePilot Assistant grounded in this repository. Choose your preferred AI engine above (Google Gemini, OpenAI GPT-4o, DeepSeek, or Hybrid Ensemble) and ask me anything!',
      timestamp: new Date()
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  selectProvider(provider: AiProvider): void {
    this.selectedProvider = provider;
    this.snackBar.open(`Switched AI Provider Engine to ${provider}`, 'Close', { duration: 2000 });
  }

  sendSuggestedQuestion(question: string): void {
    this.inputMessage = question;
    this.sendMessage();
  }

  sendMessage(): void {
    if (!this.inputMessage || this.inputMessage.trim() === '') return;

    const userText = this.inputMessage.trim();
    const provider = this.selectedProvider;
    this.inputMessage = '';

    const userMsg: ChatMessage = {
      id: 'usr_' + Date.now(),
      sender: 'USER',
      text: userText,
      timestamp: new Date()
    };
    this.messages.push(userMsg);
    this.loading = true;

    this.chatService.sendMessage(this.repoUuid, userText, provider).subscribe({
      next: (response) => {
        this.loading = false;
        const assistantMsg: ChatMessage = {
          id: 'ast_' + Date.now(),
          sender: 'ASSISTANT',
          text: response.answer,
          aiProvider: provider,
          citations: response.citations,
          timestamp: new Date()
        };
        this.messages.push(assistantMsg);
      },
      error: () => {
        this.loading = false;
        const errorMsg: ChatMessage = {
          id: 'err_' + Date.now(),
          sender: 'ASSISTANT',
          text: 'Encountered an error retrieving context for your request. Ensure repository chunks and vector embeddings have been generated.',
          timestamp: new Date()
        };
        this.messages.push(errorMsg);
      }
    });
  }

  toggleCitations(msgId: string): void {
    this.showCitations[msgId] = !this.showCitations[msgId];
  }

  private scrollToBottom(): void {
    try {
      if (this.scrollContainer) {
        this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
      }
    } catch (err) {}
  }
}
