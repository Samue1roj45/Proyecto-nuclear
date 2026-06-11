import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { AttemptSummary, ReportsSummary } from '../models';

interface AcademicExportContext {
  isAdmin: boolean;
  passThreshold: number;
  overallAverage: number;
  passProgress: number;
  pointsToPass: number;
  meetsPassThreshold: boolean;
  weakestLabel?: string;
  weakestTip?: string;
}

@Injectable({ providedIn: 'root' })
export class ReportsPdfService {
  private readonly teal: [number, number, number] = [11, 58, 66];
  private readonly cyan: [number, number, number] = [0, 212, 240];
  private readonly light: [number, number, number] = [212, 247, 255];
  private readonly warn: [number, number, number] = [255, 107, 122];

  exportAcademicSummary(
    reports: ReportsSummary,
    context: AcademicExportContext
  ): void {
    const doc = new jsPDF({ unit: 'mm', format: 'a4' });
    const title = context.isAdmin ? 'Reporte académico - Administración' : 'Reporte académico - Estudiante';

    this.paintHeader(doc, title, 'Enfoque académico y criterios de evaluación');

    let y = 42;
    y = this.sectionTitle(doc, y, 'Resumen general');
    y = this.keyValueBlock(doc, y, [
      ['Promedio clínico', `${reports.avgClinical}%`],
      ['Promedio ético', `${reports.avgEthical}%`],
      ['Promedio normativo', `${reports.avgNormative}%`],
      ['Promedio general', `${context.overallAverage}%`],
      ['Umbral de aprobación', `${context.passThreshold}%`],
      ['Progreso hacia aprobación', `${context.passProgress}%`],
      ['Tasa de aprobación', `${reports.approvalRate}%`],
      ['Casos intentados', `${reports.casesAttempted}`],
    ]);

    if (!context.meetsPassThreshold && context.pointsToPass > 0) {
      y = this.noteBox(doc, y, `Te faltan ${context.pointsToPass} puntos para alcanzar el umbral de aprobación.`, this.warn);
    }

    if (context.weakestLabel && context.weakestTip) {
      y = this.sectionTitle(doc, y + 4, 'Área prioritaria de refuerzo');
      y = this.noteBox(doc, y, `${context.weakestLabel}: ${context.weakestTip}`, this.cyan);
    }

    y = this.sectionTitle(doc, y + 4, 'Dimensiones evaluadas');
    y = this.keyValueBlock(doc, y, [
      ['Clínica', 'Intervención psicosocial, contención emocional y manejo del caso.'],
      ['Ética', 'Dignidad, confidencialidad y límites profesionales.'],
      ['Normativa', 'Rutas legales, protocolos y derivaciones institucionales.'],
    ]);

    if (reports.attempts.length > 0) {
      this.sectionTitle(doc, y + 4, 'Intentos recientes');
      autoTable(doc, {
        startY: y + 10,
        head: [['Estudiante', 'Caso', 'Puntaje', 'Estado', 'Fecha']],
        body: reports.attempts.slice(0, 12).map((attempt) => [
          attempt.studentName,
          attempt.caseTitle,
          `${attempt.totalScore}%`,
          attempt.status,
          attempt.date,
        ]),
        styles: {
          fontSize: 9,
          cellPadding: 2.5,
          textColor: this.teal,
        },
        headStyles: {
          fillColor: this.teal,
          textColor: this.light,
          fontStyle: 'bold',
        },
        alternateRowStyles: { fillColor: [232, 248, 252] },
        margin: { left: 14, right: 14 },
      });
    }

    this.paintFooter(doc);
    doc.save(`reporte-academico-${this.fileStamp()}.pdf`);
  }

  exportAttemptsReport(reports: ReportsSummary, isAdmin: boolean): void {
    const doc = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'landscape' });
    const title = isAdmin ? 'Exportación de reportes - Estudiantes' : 'Mis reportes';

    this.paintHeader(doc, title, 'Listado de intentos del simulador');

    autoTable(doc, {
      startY: 36,
      head: [['Estudiante', 'Caso', 'Intento', 'Puntaje', 'Clínico', 'Ético', 'Normativo', 'Estado', 'Fecha']],
      body: reports.attempts.map((attempt) => this.attemptRow(attempt)),
      styles: {
        fontSize: 8,
        cellPadding: 2,
        textColor: this.teal,
      },
      headStyles: {
        fillColor: this.teal,
        textColor: this.light,
        fontStyle: 'bold',
      },
      alternateRowStyles: { fillColor: [232, 248, 252] },
      margin: { left: 10, right: 10 },
    });

    const finalY = (doc as jsPDF & { lastAutoTable?: { finalY: number } }).lastAutoTable?.finalY ?? 36;
    this.keyValueBlock(doc, finalY + 8, [
      ['Promedio clínico', `${reports.avgClinical}%`],
      ['Promedio ético', `${reports.avgEthical}%`],
      ['Promedio normativo', `${reports.avgNormative}%`],
      ['Tasa de aprobación', `${reports.approvalRate}%`],
      ['Casos intentados', `${reports.casesAttempted}`],
    ]);

    this.paintFooter(doc);
    doc.save(`reportes-simulador-${this.fileStamp()}.pdf`);
  }

  private attemptRow(attempt: AttemptSummary): string[] {
    return [
      attempt.studentName,
      attempt.caseTitle,
      `${attempt.attemptNumber}`,
      `${attempt.totalScore}%`,
      `${attempt.clinicalScore}%`,
      `${attempt.ethicalScore}%`,
      `${attempt.normativeScore}%`,
      attempt.status,
      attempt.date,
    ];
  }

  private paintHeader(doc: jsPDF, title: string, subtitle: string): void {
    doc.setFillColor(...this.teal);
    doc.rect(0, 0, doc.internal.pageSize.getWidth(), 28, 'F');
    doc.setTextColor(...this.light);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text('Misión Psicosocial', 14, 12);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text(subtitle, 14, 18);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text(title, 14, 24);
  }

  private paintFooter(doc: jsPDF): void {
    const pageCount = doc.getNumberOfPages();
    for (let page = 1; page <= pageCount; page += 1) {
      doc.setPage(page);
      doc.setFontSize(8);
      doc.setTextColor(90, 120, 128);
      doc.text(
        `Generado el ${new Date().toLocaleString('es-CO')} · Página ${page} de ${pageCount}`,
        14,
        doc.internal.pageSize.getHeight() - 8
      );
    }
  }

  private sectionTitle(doc: jsPDF, y: number, label: string): number {
    if (y > 250) {
      doc.addPage();
      y = 20;
    }
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.setTextColor(...this.cyan);
    doc.text(label, 14, y);
    return y + 6;
  }

  private keyValueBlock(doc: jsPDF, y: number, rows: [string, string][]): number {
    doc.setFontSize(10);
    rows.forEach(([label, value]) => {
      if (y > 270) {
        doc.addPage();
        y = 20;
      }
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(...this.teal);
      doc.text(label, 14, y);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(40, 90, 102);
      doc.text(value, 78, y, { maxWidth: 120 });
      y += 7;
    });
    return y;
  }

  private noteBox(doc: jsPDF, y: number, text: string, color: [number, number, number]): number {
    if (y > 255) {
      doc.addPage();
      y = 20;
    }
    doc.setFillColor(232, 248, 252);
    doc.setDrawColor(...color);
    doc.rect(14, y - 4, 182, 16, 'FD');
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(...this.teal);
    doc.text(text, 18, y + 4, { maxWidth: 174 });
    return y + 18;
  }

  private fileStamp(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
