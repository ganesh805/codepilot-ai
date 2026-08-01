import { Component, OnInit, ElementRef, ViewChild, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ChatMessage } from '../../../core/models/chat.model';
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
  messages: ChatMessage[] = [];
  loading = false;
  showCitations: Record<string, boolean> = {};

  suggestedQuestions: string[] = [
    'How does authentication and JWT token validation work in this project?',
    'Explain the repository scanning and AST line chunking workflow.',
    'What REST API endpoints are exposed for repository management?'
  ];

  ngOnInit(): void {
    this.repoUuid = this.route.snapshot.paramMap.get('uuid') || '';
    // Welcome message
    this.messages.push({
      id: 'welcome',
      sender: 'ASSISTANT',
      text: 'Hello! I am your AI CodePilot Assistant grounded in this repository. Ask me any question about architecture, code features, security, or implementation details!',
      timestamp: new Date()
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  sendSuggestedQuestion(question: string): void {
    this.inputMessage = question;
    this.sendMessage();
  }

  sendMessage(): void {
    if (!this.inputMessage || this.inputMessage.trim() === '') return;

    const userText = this.inputMessage.trim();
    this.inputMessage = '';

    const userMsg: ChatMessage = {
      id: 'usr_' + Date.now(),
      sender: 'USER',
      text: userText,
      timestamp: new Date()
    };
    this.messages.push(userMsg);
    this.loading = true;

    this.chatService.sendMessage(this.repoUuid, userText).subscribe({
      next: (response) => {
        this.loading = false;
        const assistantMsg: ChatMessage = {
          id: 'ast_' + Date.now(),
          sender: 'ASSISTANT',
          text: response.answer,
          citations: response.citations,
          timestamp: new Date()
        };
        this.messages.push(assistantMsg);
      },
      error: (err) => {
        this.loading = false;
        const errorMsg: ChatMessage = {
          id: 'err_' + Date.now(),
          sender: 'ASSISTANT',
          text: 'I encountered an error retrieving context for your request. Please ensure repository chunks and vector embeddings have been generated.',
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
