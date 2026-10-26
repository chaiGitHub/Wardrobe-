import JSZip from 'jszip';
import { dataService, formatDuration } from './dataService';
import { fieldDictionaryService, FieldDefinitionItem } from './fieldDictionaryService';
import { ClothesItem, WearingReport } from '../types';

export interface NotebookLmTopicDoc {
  id: string;
  filename: string;
  title: string;
  description: string;
  category: 'catalog' | 'reports' | 'laundry' | 'glossary';
  content: string;
  wordCount: number;
  characterCount: number;
}

class NotebookLmExportService {
  /**
   * Generates the comprehensive Wardrobe Catalog and Garment Profiles markdown.
   */
  public generateWardrobeCatalogMarkdown(): string {
    const clothes = dataService.getClothes();
    const now = new Date().toISOString().split('T')[0];

    const lines: string[] = [];
    lines.push(`# Wardrobe Catalog & Garment Ergonomic Profiles`);
    lines.push(`Generated: ${now} | Source: Wardrobe Sensory & Care Management System`);
    lines.push(`Total Garments Cataloged: ${clothes.length}`);
    lines.push(``);
    lines.push(`> **NotebookLM Usage Guide**: This document contains complete inventory profiles for all cataloged garments. Each garment includes purchase economics, material composition, strap/chest silhouette evaluations, thermal comfort across air conditioning and sleep settings, and accumulated wear statistics. NOTE: All preliminary AI-generated critiques are excluded so NotebookLM performs unbiased, deep synthetic analysis directly on the pure empirical facts.`);
    lines.push(``);

    // Summary Table
    lines.push(`## 1. Wardrobe Summary Index`);
    lines.push(`| Garment Name | Category | Material | Thickness | Price | Total Wears | Wash Status | May Throw |`);
    lines.push(`| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |`);

    clothes.forEach((item) => {
      const name = item['Name'] || item.name || 'Unnamed';
      const type = item['Clothes Type'] || item.clothesType || 'Garment';
      const material = item['material'] || '(Unspecified)';
      const thickness = item['thick clothes'] || '(Unspecified)';
      const price = item['price'] !== undefined && item['price'] > 0 ? `$${item['price'].toFixed(2)}` : 'N/A';
      const stats = dataService.getGarmentStats(item.id);
      const wears = stats.totalWears || item['Total Washed Count'] || item['wearing frequency(how many times legacy washed)'] || 0;
      const status = stats.needWash ? 'Need Wash' : 'Clean';
      const declutter = item['may throw'] || 'No';

      lines.push(`| **${name}** | ${type} | ${material} | ${thickness} | ${price} | ${wears} | ${status} | ${declutter} |`);
    });

    lines.push(``);
    lines.push(`---`);
    lines.push(``);

    // Detailed Garment Profiles
    lines.push(`## 2. Individual Garment Ergonomic & Sensory Dossiers`);
    lines.push(``);

    clothes.forEach((item, index) => {
      const name = item['Name'] || item.name || `Garment #${index + 1}`;
      const type = item['Clothes Type'] || item.clothesType || 'Garment';
      const stats = dataService.getGarmentStats(item.id);
      const price = item['price'] !== undefined && item['price'] > 0 ? `$${item['price'].toFixed(2)}` : 'Not recorded';
      const totalWears = stats.totalWears || item['Total Washed Count'] || item['wearing frequency(how many times legacy washed)'] || 0;
      const costPerWear = item['price'] && totalWears > 0 ? `$${(item['price'] / totalWears).toFixed(2)}` : 'N/A';

      lines.push(`### Garment ${index + 1}: ${name}`);
      lines.push(`- **Unique System ID**: \`${item.id}\``);
      lines.push(`- **Category**: ${type}`);
      lines.push(`- **Price / Purchase Cost**: ${price}`);
      lines.push(`- **Cost Per Wear**: ${costPerWear} (over ${totalWears} recorded wear cycles)`);
      lines.push(`- **Current Laundry State**: ${stats.needWash ? '⚠️ Need Wash (in laundry queue)' : '✅ Clean'}`);
      lines.push(`- **Unwashed Accumulated Time**: ${formatDuration(stats.unwashedSeconds)} (${stats.unwashedSeconds} seconds)`);
      lines.push(`- **Last Worn Date**: ${item['Last Worn Date'] || item.lastWornDate || 'No wear date recorded'}`);
      lines.push(`- **Last Washed Date**: ${item['Last Washed'] || item.lastWashed || 'No wash date recorded'}`);
      lines.push(``);

      lines.push(`#### Physical Attributes & Aesthetics`);
      lines.push(`- **Material Composition**: ${item['material'] || 'Not specified'}`);
      lines.push(`- **Fabric Thickness**: ${item['thick clothes'] || 'Not specified'}`);
      lines.push(`- **Dress Style / Length**: ${item['long dress'] || 'Not specified'}`);
      lines.push(`- **Shoulder Strap Cut (\`diao dai\`)**: ${item['diao dai'] || 'Not specified'}`);
      lines.push(`- **Aesthetic Rating (\`beautiful\`)**: ${item['beautiful'] || 'Not specified'}`);
      if (item['beatiful reason']) {
        lines.push(`- **Aesthetic Rationale (\`beatiful reason\`)**: ${item['beatiful reason']}`);
      }
      lines.push(`- **Declutter Flag (\`may throw\`)**: ${item['may throw'] || 'Keep / No discard flag'}`);
      lines.push(``);

      lines.push(`#### Silhouette & Ergonomic Breast Fit`);
      lines.push(`- **Front Seated Fit (with Push-up Bra \`sit breast ok: With 聚拢bra\`)**: ${item['sit breast ok: With 聚拢bra'] || 'Not evaluated'}`);
      lines.push(`- **Front Seated Fit (with Side-open Bra \`sit breast ok: with side out bra\`)**: ${item['sit breast ok: with side out bra'] || 'Not evaluated'}`);
      lines.push(`- **Side Contour Shape (\`side breast ok\`)**: ${item['side breast ok'] || 'Not evaluated'}`);
      lines.push(`- **Center Chest / Gore Pressure (\`center chest\`)**: ${item['center chest'] || 'Not evaluated'}`);
      lines.push(``);

      lines.push(`#### Thermal & Climate Comfort`);
      lines.push(`- **Midday Peak Sun & Heat (\`Hot at noon\`)**: ${item['Hot at noon'] || 'Not evaluated'}`);
      lines.push(`- **Dampness / Moisture Retention (\`wet\`)**: ${item['wet'] || 'Not evaluated'}`);
      if (item['wet reason']) {
        lines.push(`- **Moisture Cause / Drying Note (\`wet reason\`)**: ${item['wet reason']}`);
      }
      lines.push(`- **With Air Conditioning (Hot Feeling)**: ${item['when with aircond: hot feeling'] || 'Not evaluated'}`);
      lines.push(`- **With Air Conditioning (Leg Feeling)**: ${item['when with aircond: leg feeling'] || 'Not evaluated'}`);
      lines.push(`- **Without Air Conditioning (Hot Feeling)**: ${item['when without aircond: hot feeling'] || 'Not evaluated'}`);
      lines.push(`- **Without Air Conditioning (Leg Feeling)**: ${item['when without aircond: leg feeling'] || 'Not evaluated'}`);
      lines.push(`- **Sleep Under Fan (Upper Body Feeling)**: ${item['when sleep under fan: hot feeling'] || 'Not evaluated'}`);
      lines.push(`- **Sleep Under Fan (Leg Feeling)**: ${item['when sleep under fan: leg feeling'] || 'Not evaluated'}`);
      lines.push(``);

      if (item['Descriptions'] || item['Descriptions 2'] || item['last comment or action']) {
        lines.push(`#### Wearer Observation Notes`);
        if (item['Descriptions']) lines.push(`- **Primary Description**: ${item['Descriptions']}`);
        if (item['Descriptions 2']) lines.push(`- **Ergonomic Notes**: ${item['Descriptions 2']}`);
        if (item['last comment or action']) lines.push(`- **Latest Action / Verdict**: ${item['last comment or action']}`);
        lines.push(``);
      }

      lines.push(`---`);
      lines.push(``);
    });

    return lines.join('\n');
  }

  /**
   * Generates the Wearing Sensory Reports and Journal markdown.
   */
  public generateWearingReportsMarkdown(): string {
    const reports = dataService.getReports();
    const junctions = dataService.getJunctions();
    const clothes = dataService.getClothes();
    const clothesMap = new Map(clothes.map((c) => [c.id, c]));
    const now = new Date().toISOString().split('T')[0];

    const sortedReports = [...reports].sort((a, b) => {
      const dateA = a.date || a['Wear Date'] || '';
      const dateB = b.date || b['Wear Date'] || '';
      return dateB.localeCompare(dateA);
    });

    const lines: string[] = [];
    lines.push(`# Wearing Sensory Reports & Wear Journal`);
    lines.push(`Generated: ${now} | Source: Wardrobe Sensory & Care Management System`);
    lines.push(`Total Wear Sessions Recorded: ${sortedReports.length}`);
    lines.push(``);
    lines.push(`> **NotebookLM Usage Guide**: This document contains chronological wear sessions detailing real-world outfit performance. Each entry records the worn outfit, bra pairing, session duration, shoulder strap pain, breast silhouette comfort, sweating/dampness, nighttime sleep comfort under fans, and wash confirmations. NOTE: All in-app AI diagnostic critiques are excluded so NotebookLM operates solely on pure, ground-truth empirical wear telemetry.`);
    lines.push(``);

    // Summary Index Table
    lines.push(`## 1. Wear Sessions Chronological Index`);
    lines.push(`| Date | Report Title | Clothes Worn | Bra Worn | Duration | Wash Confirmed |`);
    lines.push(`| :--- | :--- | :--- | :--- | :--- | :--- |`);

    sortedReports.forEach((r) => {
      const date = r.date || r['Wear Date'] || 'Unspecified date';
      const title = r.title || r['Report Name'] || 'Wear Report';
      const clothesName = r.clothesTitle || r['Clothes Title'] || '(Unlinked)';
      const braName = r.braTitle || r['Bra Title'] || '(Unlinked)';
      const duration = r.durationSeconds ? formatDuration(r.durationSeconds) : (r.duration || 'N/A');
      const washed = r.clothesWashed || r.braWashed ? 'Yes' : 'No';

      lines.push(`| ${date} | **${title}** | ${clothesName} | ${braName} | ${duration} | ${washed} |`);
    });

    lines.push(``);
    lines.push(`---`);
    lines.push(``);

    // Detailed Report Entries
    lines.push(`## 2. Detailed Empirical Wear Logs & Sensory Feedback`);
    lines.push(``);

    sortedReports.forEach((report, index) => {
      const date = report.date || report['Wear Date'] || 'Unspecified Date';
      const title = report.title || report['Report Name'] || `Session #${index + 1}`;
      const durationFormatted = report.durationSeconds ? formatDuration(report.durationSeconds) : (report.duration || 'Unspecified');

      lines.push(`### Wear Session ${index + 1}: ${title} (${date})`);
      lines.push(`- **Report ID**: \`${report.id}\``);
      lines.push(`- **Date of Wear**: ${date}`);
      lines.push(`- **Reported Time Period**: ${report.timePeriod || report['Time Period'] || 'Not specified'}`);
      lines.push(`- **Recorded Duration**: ${durationFormatted} (${report.durationSeconds || 0} seconds)`);
      lines.push(`- **Primary Garment**: ${report.clothesTitle || report['Clothes Title'] || 'None'}`);
      lines.push(`- **Undergarment / Bra**: ${report.braTitle || report['Bra Title'] || 'None'}`);
      lines.push(`- **Clothes Washed After Session**: ${report.clothesWashed ? '✅ Confirmed Washed' : '❌ Unwashed / Returned to closet'}`);
      lines.push(`- **Bra Washed After Session**: ${report.braWashed ? '✅ Confirmed Washed' : '❌ Unwashed'}`);
      lines.push(``);

      lines.push(`#### Sensory Fit & Ergonomic Observations`);
      lines.push(`- **Center Chest / Mid-Breast (\`midBreast\` / \`mid breast\`)**: ${report.midBreast || report['mid breast'] || report['midBreast'] || 'No observation recorded'}`);
      lines.push(`- **Shoulder Feel & Strap Pressure (\`shoulderFeel\` / \`shoulder feel\` / \`diao dai\`)**: ${report.shoulderFeel || report['shoulder feel'] || 'No shoulder observation recorded'}`);
      lines.push(`- **Leg Comfort & Dress vs Pants (\`legFeel\` / \`leg feel\`)**: ${report.legFeel || report['leg feel'] || 'No leg observation recorded'}`);
      lines.push(``);

      lines.push(`#### Thermal, Sweating & Climate Observations`);
      lines.push(`- **Sweat & Dampness (\`sweat\` / \`sweating\`)**: ${report.sweat || report['sweat'] || 'No sweat recorded'}`);
      lines.push(`- **Sleep Under Fan Comfort (\`sleepUnderFan\`)**: ${report.sleepUnderFan || report['sleep under fan'] || 'Not evaluated'}`);
      lines.push(`- **Air Conditioning Comfort (\`withAircond\`)**: ${report.withAircond || report['with aircond'] || 'Not evaluated'}`);
      lines.push(``);

      // Associated Junction records
      const linkedJunctions = junctions.filter((j) => {
        const rId = j.wearingReportId || (typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : String(j['wearing report id']));
        return rId === report.id;
      });
      if (linkedJunctions.length > 0) {
        lines.push(`#### Associated Garment Junction Linkages`);
        linkedJunctions.forEach((j) => {
          const cId = j.clothesId || (typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : String(j['clothes(new)']));
          const item = clothesMap.get(cId);
          const itemName = item ? (item['Name'] || item.name) : (j.clothesTitle || cId);
          const isWash = j['wash stated'] === 'wash';
          const washStatus = isWash ? `Washed on ${j['wash date'] || j.washDate || 'unspecified date'}` : 'Unwashed in this wear session';
          lines.push(`- Garment: **${itemName}** — ${washStatus}`);
        });
        lines.push(``);
      }

      if (report.notes || report['Notes'] || report['generalReportDetail']) {
        lines.push(`#### Wearer Subjective Notes`);
        lines.push(report.notes || report['Notes'] || report['generalReportDetail'] || '');
        lines.push(``);
      }

      lines.push(`---`);
      lines.push(``);
    });

    return lines.join('\n');
  }

  /**
   * Generates the Laundry Care and Maintenance Guide markdown.
   */
  public generateLaundryCareMarkdown(): string {
    const clothes = dataService.getClothes();
    const junctions = dataService.getJunctions();
    const reports = dataService.getReports();
    const now = new Date().toISOString().split('T')[0];

    const needWashList: ClothesItem[] = [];
    const cleanList: ClothesItem[] = [];

    clothes.forEach((c) => {
      const stats = dataService.getGarmentStats(c.id);
      if (stats.needWash) {
        needWashList.push(c);
      } else {
        cleanList.push(c);
      }
    });

    const lines: string[] = [];
    lines.push(`# Laundry Care, Wash Backlog & Garment Maintenance Guide`);
    lines.push(`Generated: ${now} | Source: Wardrobe Sensory & Care Management System`);
    lines.push(`Total Wardrobe Items: ${clothes.length} | Items Needing Wash: ${needWashList.length} | Clean Items: ${cleanList.length}`);
    lines.push(``);
    lines.push(`> **NotebookLM Usage Guide**: This document details laundry prioritization, fabric maintenance recommendations, unwashed accumulated hours, and historical wash cycles. Use this source in NotebookLM to plan wash routines by fabric composition, determine which garments require delicate or hand washing, and cross-reference wear duration with washing urgency.`);
    lines.push(``);

    // Urgent Wash Backlog Section
    lines.push(`## 1. Urgent Wash Backlog (Requires Washing)`);
    lines.push(`Total Backlog: **${needWashList.length} items**`);
    lines.push(``);

    if (needWashList.length === 0) {
      lines.push(`*All wardrobe garments are currently marked clean! Zero wash backlog.*`);
      lines.push(``);
    } else {
      lines.push(`| Garment Name | Type | Material | Thickness | Accumulated Unwashed Wear | Last Worn | Care Recommendation |`);
      lines.push(`| :--- | :--- | :--- | :--- | :--- | :--- | :--- |`);

      needWashList.forEach((item) => {
        const name = item['Name'] || item.name || 'Unnamed';
        const type = item['Clothes Type'] || item.clothesType || 'Garment';
        const material = item['material'] || 'Unknown material';
        const thickness = item['thick clothes'] || 'Standard';
        const stats = dataService.getGarmentStats(item.id);
        const duration = formatDuration(stats.unwashedSeconds);
        const lastWorn = item['Last Worn Date'] || item.lastWornDate || 'N/A';

        let careTip = 'Standard machine wash cold; air dry recommended.';
        const matLower = material.toLowerCase();
        if (matLower.includes('chiffon') || matLower.includes('lace') || matLower.includes('silk')) {
          careTip = 'Delicate/hand wash cold; use mesh laundry bag; never tumble dry.';
        } else if (matLower.includes('thick') || thickness.toLowerCase().includes('thick')) {
          careTip = 'Thorough rinse to prevent sweat odor retention; dry completely under ventilation.';
        } else if (type.toLowerCase().includes('bra')) {
          careTip = 'Hand wash with mild detergent to preserve underwire and center gore structure.';
        }

        lines.push(`| **${name}** | ${type} | ${material} | ${thickness} | ${duration} | ${lastWorn} | ${careTip} |`);
      });
      lines.push(``);
    }

    lines.push(`---`);
    lines.push(``);

    // Clean Wardrobe Inventory
    lines.push(`## 2. Clean Wardrobe Inventory`);
    lines.push(`| Garment Name | Type | Material | Total Wash Cycles | Last Washed Date |`);
    lines.push(`| :--- | :--- | :--- | :--- | :--- |`);

    cleanList.forEach((item) => {
      const name = item['Name'] || item.name || 'Unnamed';
      const type = item['Clothes Type'] || item.clothesType || 'Garment';
      const material = item['material'] || 'Unspecified';
      const stats = dataService.getGarmentStats(item.id);
      const totalWashes = stats.totalWears || item['Total Washed Count'] || item['wearing frequency(how many times legacy washed)'] || 0;
      const lastWashed = item['Last Washed'] || item.lastWashed || 'None on record';

      lines.push(`| **${name}** | ${type} | ${material} | ${totalWashes} | ${lastWashed} |`);
    });

    lines.push(``);
    lines.push(`---`);
    lines.push(``);

    // Fabric Care Strategic Matrix
    lines.push(`## 3. Fabric Care & Maintenance Reference Matrix`);
    lines.push(`- **Chiffon & Lace Outfits** (e.g. \`pink diao dai chiffon\`):`);
    lines.push(`  - Sensitive to abrasive friction and high heat.`);
    lines.push(`  - Recommend cold wash (<30°C) with low spin cycle (under 600 RPM) inside protective laundry nets.`);
    lines.push(`  - Always hang to dry in indirect shade to prevent fiber stiffening.`);
    lines.push(`- **Thick & Warm Fabrics** (e.g. \`blue diao dai thick\`, \`yellow diao dai\`):`);
    lines.push(`  - Tend to absorb sweat and stay damp longer during humid periods.`);
    lines.push(`  - Prioritize wash after ~8 to 12 hours of accumulated wear to prevent bacterial odor buildup.`);
    lines.push(`  - Ensure full drying before closet storage.`);
    lines.push(`- **Bras & Ergonomic Undergarments** (e.g. \`purple old bra\`, \`small bra\`, \`聚拢bra\`):`);
    lines.push(`  - Avoid heat dryers which degrade elastane and cause strap slackening.`);
    lines.push(`  - Reshape molded cups immediately after washing while still damp.`);

    return lines.join('\n');
  }

  /**
   * Generates the Bilingual Field Definitions and Glossary markdown.
   */
  public generateFieldDefinitionsMarkdown(): string {
    const definitions = fieldDictionaryService.getDefinitions();
    const now = new Date().toISOString().split('T')[0];

    const lines: string[] = [];
    lines.push(`# Wardrobe Sensory Field Definitions & Bilingual Glossary`);
    lines.push(`Generated: ${now} | Source: Wardrobe Sensory & Care Management System`);
    lines.push(`Total Term Definitions: ${definitions.length}`);
    lines.push(``);
    lines.push(`> **NotebookLM Usage Guide**: This foundational glossary provides ground-truth explanations for all domain-specific terminology used throughout the wardrobe app. When querying NotebookLM about strap strain (*diao dai*), seated bust contour (*sit breast ok*), noon heat breathability (*Hot at noon*), or declutter ratings (*may throw*), this document serves as the authoritative definition dictionary.`);
    lines.push(``);

    const categories: ('clothes' | 'wearingReport' | 'junction')[] = ['clothes', 'wearingReport', 'junction'];
    const categoryTitles: Record<string, string> = {
      clothes: 'Garment Attributes & Cut Definitions (Clothes)',
      wearingReport: 'Wearing Session & Sensory Observations (Reports)',
      junction: 'Wash & Lifecycle Junction Metrics'
    };

    categories.forEach((cat) => {
      const catDefs = definitions.filter((d) => d.category === cat);
      if (catDefs.length === 0) return;

      lines.push(`## ${categoryTitles[cat] || cat}`);
      lines.push(``);

      catDefs.forEach((def, index) => {
        lines.push(`### ${index + 1}. \`${def.fieldNameEn}\` (${def.fieldNameCn})`);
        lines.push(`- **English Description**: ${def.descriptionEn || 'No English description provided.'}`);
        lines.push(`- **Chinese Description**: ${def.descriptionCn || '无中文说明。'}`);
        lines.push(`- **Category**: ${def.category}`);
        lines.push(`- **AI Analysis Enabled**: ${def.enabledForAi !== false ? 'Yes' : 'No'}`);

        if (def.typicalValues && def.typicalValues.length > 0) {
          lines.push(``);
          lines.push(`| Value (EN) | Value (CN) | Evaluation Meaning |`);
          lines.push(`| :--- | :--- | :--- |`);
          def.typicalValues.forEach((val) => {
            const vEn = val.valueEn || '';
            const vCn = val.valueCn || '';
            const meaning = val.meaningCn || val.meaningEn || '';
            lines.push(`| \`${vEn}\` | ${vCn} | ${meaning} |`);
          });
        }
        lines.push(``);
        lines.push(`---`);
        lines.push(``);
      });
    });

    return lines.join('\n');
  }

  /**
   * Compiles all 4 topical documents.
   */
  public getAllTopicDocs(): NotebookLmTopicDoc[] {
    const catalogMd = this.generateWardrobeCatalogMarkdown();
    const reportsMd = this.generateWearingReportsMarkdown();
    const laundryMd = this.generateLaundryCareMarkdown();
    const glossaryMd = this.generateFieldDefinitionsMarkdown();

    const countWords = (text: string) => {
      return (text.match(/[\w\d'-]+|[\u4e00-\u9fa5]/g) || []).length;
    };

    return [
      {
        id: 'catalog',
        filename: '01_Wardrobe_Catalog_and_Garment_Profiles.md',
        title: 'Wardrobe Catalog & Garment Profiles',
        description: 'Comprehensive inventory of all garments with prices, cuts, straps, chest fit, and thermal ratings.',
        category: 'catalog',
        content: catalogMd,
        wordCount: countWords(catalogMd),
        characterCount: catalogMd.length
      },
      {
        id: 'reports',
        filename: '02_Wearing_Sensory_Reports_and_Journal.md',
        title: 'Wearing Sensory Reports & Journal',
        description: 'Chronological wear logs recording bra pairings, shoulder strain, sweating, and nighttime fan sleep.',
        category: 'reports',
        content: reportsMd,
        wordCount: countWords(reportsMd),
        characterCount: reportsMd.length
      },
      {
        id: 'laundry',
        filename: '03_Laundry_Care_and_Maintenance_Guide.md',
        title: 'Laundry Care & Maintenance Guide',
        description: 'Urgent wash backlog, accumulated unwashed wear hours, and fabric-specific laundry care instructions.',
        category: 'laundry',
        content: laundryMd,
        wordCount: countWords(laundryMd),
        characterCount: laundryMd.length
      },
      {
        id: 'glossary',
        filename: '04_Sensory_Field_Definitions_and_Glossary.md',
        title: 'Sensory Field Definitions & Glossary',
        description: 'Bilingual dictionary explaining terms like diao dai, sit breast ok, and Hot at noon for AI grounding.',
        category: 'glossary',
        content: glossaryMd,
        wordCount: countWords(glossaryMd),
        characterCount: glossaryMd.length
      }
    ];
  }

  /**
   * Generates a bundled ZIP archive containing all 4 markdown documents plus a NotebookLM Quickstart Guide.
   */
  public async generateNotebookLmKnowledgeZip(): Promise<Blob> {
    const zip = new JSZip();
    const docs = this.getAllTopicDocs();

    // Add all 4 markdown files
    docs.forEach((doc) => {
      zip.file(doc.filename, doc.content);
    });

    // Add a helpful README with recommended NotebookLM prompts
    const readmeContent = `# Google NotebookLM Wardrobe Knowledge Pack
Generated: ${new Date().toISOString()}

## How to use these files in Google NotebookLM:
1. Open Google NotebookLM (https://notebooklm.google.com).
2. Create a new notebook titled: "My Wardrobe & Sensory Wear Analytics".
3. Click "Add Source" -> "File Upload".
4. Upload all 4 markdown (.md) files included in this ZIP archive:
   - 01_Wardrobe_Catalog_and_Garment_Profiles.md
   - 02_Wearing_Sensory_Reports_and_Journal.md
   - 03_Laundry_Care_and_Maintenance_Guide.md
   - 04_Sensory_Field_Definitions_and_Glossary.md

## Dataset Purity Guarantee:
- 100% Pure Empirical Data: Contains verified garment specifications, wearer sensory evaluations, timestamps, duration, and care routines.
- Zero In-App AI Bias: All preliminary AI-generated critiques have been completely excluded so Google NotebookLM performs unbiased synthesis and high-fidelity Audio Overviews directly from primary ground-truth facts.

## High-Value Questions & Prompts to Ask NotebookLM:
- "Create an Audio Overview summarizing the wearer's most comfortable outfits and greatest fit challenges."
- "Which garments cause shoulder irritation or strap digging ('diao dai'), and which bras best resolve center-chest gore pressure?"
- "Compare daytime heat performance versus sleeping under a fan for thick dresses."
- "Based on the wear records and prices, calculate which garments offer the highest practical value per wear."
- "What items are currently flagged with 'may throw', and what sensory reasons explain why the wearer dislikes them?"
- "Generate a weekly laundry care schedule grouped by delicate chiffon/lace versus durable cotton garments."
`;
    zip.file('README_NotebookLM_Guide.txt', readmeContent);

    return await zip.generateAsync({
      type: 'blob',
      compression: 'DEFLATE',
      compressionOptions: { level: 9 }
    });
  }

  /**
   * Helper to trigger a browser file download for a text file or Blob.
   */
  public downloadFile(filename: string, content: string | Blob, mimeType = 'text/markdown;charset=utf-8') {
    const blob = typeof content === 'string' ? new Blob([content], { type: mimeType }) : content;
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }
}

export const notebookLmExportService = new NotebookLmExportService();
