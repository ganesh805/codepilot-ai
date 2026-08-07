import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { OptimizerService } from '../../../core/services/optimizer.service';
import { CodeOptimizerResponse } from '../../../core/models/optimizer.model';

@Component({
  selector: 'app-code-optimizer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './code-optimizer.component.html',
  styleUrls: ['./code-optimizer.component.scss']
})
export class CodeOptimizerComponent implements OnInit {
  private optimizerService = inject(OptimizerService);

  inputCode = '';
  selectedLanguage = 'Auto-Detect';
  languages = [
    'Auto-Detect', 'Java', 'Python', 'C', 'C++', 'C#',
    'JavaScript', 'TypeScript', 'Go', 'Rust', 'Kotlin',
    'Swift', 'PHP', 'Ruby', 'Dart', 'Scala', 'R', 'SQL', 'Bash'
  ];

  analyzing = false;
  currentResponse: CodeOptimizerResponse | null = null;
  history: CodeOptimizerResponse[] = [];
  activeTab: 'report' | 'code' | 'complexity' = 'report';
  copied = false;

  ngOnInit(): void {
    this.loadHistory();
    // Default sample DSA code for quick testing
    this.inputCode = `// Sample: Find Duplicate Elements in Array (Nested Loop O(N²))
public static List<Integer> findDuplicates(List<Integer> items) {
    List<Integer> duplicates = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
        for (int j = i + 1; j < items.size(); j++) {
            if (items.get(i).equals(items.get(j)) && !duplicates.contains(items.get(i))) {
                duplicates.add(items.get(i));
            }
        }
    }
    return duplicates;
}`;
  }

  loadHistory(): void {
    this.optimizerService.getHistory().subscribe({
      next: (data) => this.history = data,
      error: (err) => console.error('Failed to load history', err)
    });
  }

  runOptimization(): void {
    if (!this.inputCode || !this.inputCode.trim()) return;

    this.analyzing = true;
    this.currentResponse = null;

    this.optimizerService.optimizeCode({
      code: this.inputCode,
      language: this.selectedLanguage
    }).subscribe({
      next: (response) => {
        this.currentResponse = response;
        this.analyzing = false;
        this.loadHistory();
      },
      error: (err) => {
        console.error('Optimization error', err);
        this.analyzing = false;
      }
    });
  }

  loadSample(type: 'dsa' | 'python_dp' | 'sql' | 'string_builder'): void {
    if (type === 'dsa') {
      this.inputCode = `// Sample: Find Duplicate Elements in Array (Nested Loop O(N²))
public static List<Integer> findDuplicates(List<Integer> items) {
    List<Integer> duplicates = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
        for (int j = i + 1; j < items.size(); j++) {
            if (items.get(i).equals(items.get(j)) && !duplicates.contains(items.get(i))) {
                duplicates.add(items.get(i));
            }
        }
    }
    return duplicates;
}`;
    } else if (type === 'python_dp') {
      this.inputCode = `# Sample: Naive Recursive Fibonacci in Python (O(2^N))
def fibonacci(n: int) -> int:
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)`;
    } else if (type === 'sql') {
      this.inputCode = `-- Sample: Unindexed Table Join & SELECT *
SELECT * 
FROM users u 
JOIN orders o ON u.id = o.user_id 
WHERE u.status = 'ACTIVE' AND o.total_price > 500
ORDER BY o.created_at DESC;`;
    } else if (type === 'string_builder') {
      this.inputCode = `// Sample: String Concatenation in Loop (O(N²))
public String buildCsvString(List<String> records) {
    String csv = "";
    for (String record : records) {
        csv += record + ", ";
    }
    return csv;
}`;
    }
  }

  copyOptimizedCode(): void {
    if (this.currentResponse?.optimizedCode) {
      navigator.clipboard.writeText(this.currentResponse.optimizedCode);
      this.copied = true;
      setTimeout(() => this.copied = false, 2000);
    }
  }

  selectHistory(item: CodeOptimizerResponse): void {
    this.currentResponse = item;
    this.inputCode = item.rawCode;
  }
}
